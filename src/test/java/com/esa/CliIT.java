package com.esa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliIT
{
    @TempDir
    Path workingDirectory;

    @Test
    void runsTheAssignmentSampleWithDefaultAndExplicitOutputDirectories() throws Exception
    {
        Path sampleDirectory = Path.of("src/test/resources/sample");
        Files.copy(sampleDirectory.resolve("events.txt"), workingDirectory.resolve("events.txt"));

        for (String output : List.of(".", "reports/daily"))
        {
            Process process = output.equals(".") ? launch("--file", "events.txt")
                    : launch("--file", "events.txt", "--outdir", output);

            assertEquals(0, process.exitValue(), diagnostics());
            assertEquals("", diagnostics());
            for (String filename : List.of("sensors.json", "devices.json", "access.json"))
            {
                assertEquals(JsonParser.parseString(Files.readString(sampleDirectory.resolve(filename))),
                        JsonParser.parseString(Files.readString(workingDirectory.resolve(output).resolve(filename))),
                        output + "/" + filename);
            }
        }
    }

    @Test
    void exitsWithAnErrorMessageWhenTheInputFileDoesNotExist() throws Exception
    {
        Process process = launch("--file", "missing.txt");

        assertNotEquals(0, process.exitValue());
        assertTrue(diagnostics().contains("Input file does not exist: missing.txt"), diagnostics());
    }

    @Test
    void reportsMissingFileArgumentsOnStderrAndExitsNonZero() throws Exception
    {
        var cases = java.util.Map.of(List.<String>of(), "Missing required argument: --file",
                List.of("--file"), "Missing value for argument: --file");
        for (var scenario : cases.entrySet())
        {
            Process process = launch(scenario.getKey().toArray(String[]::new));

            assertNotEquals(0, process.exitValue());
            assertTrue(diagnostics().contains(scenario.getValue()), diagnostics());
        }
    }

    @Test
    void reportsOutputWriteFailuresOnStderrAndExitsNonZero() throws Exception
    {
        Files.createFile(workingDirectory.resolve("events.txt"));
        Files.createDirectories(workingDirectory.resolve("reports/sensors.json"));

        Process process = launch("--file", "events.txt", "--outdir", "reports");

        assertNotEquals(0, process.exitValue());
        assertTrue(diagnostics().startsWith("Error: "), diagnostics());
        assertTrue(diagnostics().contains("sensors.json"), diagnostics());
    }

    @Test
    void rejectsInputOutputCollisionsBeforeChangingAnyReports() throws Exception
    {
        String input = "sensor=temp reading=12.5\n";
        for (String alias : List.of("direct", "symlink", "hardlink"))
        {
            for (String report : List.of("sensors", "devices", "access"))
            {
                Path directory = Files.createDirectory(workingDirectory.resolve(alias + report));
                Path source = directory.resolve(alias.equals("direct") ? report + ".json" : "events.txt");
                Files.writeString(source, input);
                for (String name : List.of("sensors", "devices", "access"))
                {
                    Path output = directory.resolve(name + ".json");
                    if (!name.equals(report))
                    {
                        Files.writeString(output, "existing report");
                    }
                    else if (alias.equals("symlink"))
                    {
                        Files.createSymbolicLink(output, source.getFileName());
                    }
                    else if (alias.equals("hardlink"))
                    {
                        Files.createLink(output, source);
                    }
                }
                Process process = launch("--file", source.toString(), "--outdir", directory.toString());
                assertNotEquals(0, process.exitValue(), alias + report);
                assertTrue(diagnostics().contains("Input file conflicts with output"), diagnostics());
                assertEquals(input, Files.readString(source));
                for (String name : List.of("sensors", "devices", "access"))
                {
                    assertEquals(name.equals(report) ? input : "existing report",
                            Files.readString(directory.resolve(name + ".json")));
                }
            }
        }
    }

    @Test
    void preservesLargeSensorValuesInTheJsonReport() throws Exception
    {
        Files.writeString(workingDirectory.resolve("events.txt"), "sensor=large reading=1000000000000000000");
        Process process = launch("--file", "events.txt");
        assertEquals(0, process.exitValue(), diagnostics());
        var stats = JsonParser.parseString(Files.readString(workingDirectory.resolve("sensors.json")))
                .getAsJsonObject().getAsJsonObject("large");
        for (String key : List.of("minimum", "median", "average", "maximum"))
        {
            assertEquals(new java.math.BigDecimal("1000000000000000000"), stats.get(key).getAsBigDecimal());
        }
    }

    @Test
    void rejectsReportsThatAliasEachOtherBeforeWriting() throws Exception
    {
        for (String alias : List.of("symlink", "hardlink"))
        {
            Path directory = Files.createDirectory(workingDirectory.resolve(alias));
            Path source = directory.resolve("events.txt");
            Files.writeString(source, "sensor=temp reading=20\nseverity=INFO device=lock\n");
            Path sensors = directory.resolve("sensors.json");
            Path devices = directory.resolve("devices.json");
            Files.writeString(sensors, "existing report");
            Files.writeString(directory.resolve("access.json"), "existing access report");
            if (alias.equals("symlink"))
            {
                Files.createSymbolicLink(devices, sensors.getFileName());
            }
            else
            {
                Files.createLink(devices, sensors);
            }

            Process process = launch("--file", source.toString(), "--outdir", directory.toString());

            assertNotEquals(0, process.exitValue(), alias);
            assertTrue(diagnostics().contains("Output files conflict"), diagnostics());
            assertEquals("existing report", Files.readString(sensors));
            assertEquals("existing report", Files.readString(devices));
            assertEquals("existing access report", Files.readString(directory.resolve("access.json")));
        }
    }

    @Test
    void rejectsDanglingReportSymlinksBeforeCreatingTheirTargets() throws Exception
    {
        for (String name : List.of("sensors", "devices", "access"))
        {
            Path directory = Files.createDirectory(workingDirectory.resolve(name));
            Path source = directory.resolve("events.txt");
            String input = "sensor=temp reading=20\nseverity=INFO device=lock\n";
            Files.writeString(source, input);
            String targetName = name.equals("devices") ? "sensors" : "devices";
            Path target = directory.resolve(targetName + ".json");
            Path link = directory.resolve(name + ".json");
            Files.createSymbolicLink(link, target.getFileName());
            String otherName = name.equals("access") ? "sensors" : "access";
            Path existingReport = directory.resolve(otherName + ".json");
            Files.writeString(existingReport, "existing report");

            Process process = launch("--file", source.toString(), "--outdir", directory.toString());

            assertNotEquals(0, process.exitValue(), name);
            assertTrue(diagnostics().contains("Output is a dangling symbolic link"), diagnostics());
            assertFalse(Files.exists(target));
            assertEquals(target.getFileName(), Files.readSymbolicLink(link));
            assertEquals("existing report", Files.readString(existingReport));
            assertEquals(input, Files.readString(source));
        }
    }

    @Test
    void skipsBrokenQuotesAndWritesFiniteStatisticsForExtremeReadings() throws Exception
    {
        Files.writeString(workingDirectory.resolve("events.txt"),
                "severity=INFO device=\"broken lock\n"
                + "sensor=opposite reading=1e155\nsensor=opposite reading=-1e155\n"
                + "sensor=identical reading=1e308\nsensor=identical reading=1e308\n"
                + "severity=INFO device=lock\n");

        Process process = launch("--file", "events.txt");

        assertEquals(0, process.exitValue(), diagnostics());
        var sensors = JsonParser.parseString(Files.readString(workingDirectory.resolve("sensors.json")))
                .getAsJsonObject();
        assertEquals(1e155, sensors.getAsJsonObject("opposite").get("std_dev").getAsDouble());
        assertEquals(0.0, sensors.getAsJsonObject("opposite").get("average").getAsDouble());
        assertEquals(1e308, sensors.getAsJsonObject("identical").get("average").getAsDouble());
        assertEquals(1e308, sensors.getAsJsonObject("identical").get("median").getAsDouble());
        assertEquals(0.0, sensors.getAsJsonObject("identical").get("std_dev").getAsDouble());
        var devices = JsonParser.parseString(Files.readString(workingDirectory.resolve("devices.json")))
                .getAsJsonObject();
        assertEquals(1, devices.getAsJsonObject("by_device").size());
        assertTrue(devices.getAsJsonObject("by_device").has("lock"));
    }

    private Process launch(String... arguments) throws Exception
    {
        Path jar = Path.of("target/analyzer.jar").toAbsolutePath();
        List<String> command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-jar", jar.toString()));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectOutput(workingDirectory.resolve("console.txt").toFile())
                .redirectError(workingDirectory.resolve("error.txt").toFile())
                .start();
        if (!process.waitFor(15, TimeUnit.SECONDS))
        {
            process.destroyForcibly();
            throw new AssertionError("CLI did not exit within 15 seconds");
        }
        return process;
    }

    private String diagnostics() throws Exception
    {
        return Files.readString(workingDirectory.resolve("error.txt"));
    }
}
