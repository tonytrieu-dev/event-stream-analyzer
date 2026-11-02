package com.esa.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonWriterTest
{
    @TempDir
    Path outputDirectory;

    private final OutputWriter writer = new JsonWriter();

    private Path inputFile() throws IOException
    {
        return Files.createFile(outputDirectory.resolve("events.txt"));
    }

    @Test
    void indentsNestedObjectsWithTwoSpaces() throws IOException
    {
        writer.write(inputFile(), outputDirectory, Map.of("sensors", Map.of("temp", Map.of("unit", "celsius"))));

        assertEquals("{\n  \"temp\": {\n    \"unit\": \"celsius\"\n  }\n}",
                Files.readString(outputDirectory.resolve("sensors.json")));
    }

    @Test
    void writesWholeNumbersWithoutDecimalPointsOrExponents() throws IOException
    {
        writer.write(inputFile(), outputDirectory,
                Map.of("sensors", Map.of("values", List.of(40.0, -20.0, -0.0, 40, 1e18))));

        assertEquals("[40,-20,0,40,1000000000000000000]", JsonParser.parseString(
                Files.readString(outputDirectory.resolve("sensors.json")))
                .getAsJsonObject().getAsJsonArray("values").toString());
    }

    @Test
    void roundsFractionalNumbersAndDropsDecimalsWhenTheResultIsWhole() throws IOException
    {
        writer.write(inputFile(), outputDirectory,
                Map.of("sensors", Map.of("values", List.of(21.64, 21.66, -2.36, 21.96))));

        assertEquals("[21.6,21.7,-2.4,22]", JsonParser.parseString(
                Files.readString(outputDirectory.resolve("sensors.json")))
                .getAsJsonObject().getAsJsonArray("values").toString());
    }

    @Test
    void rejectsFreshReportPathAliasesBeforeCreatingAnyOutput() throws IOException
    {
        Path input = Files.writeString(outputDirectory.resolve("events.txt"), "original input");
        Path reportsDirectory = outputDirectory.resolve("new reports");
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        reports.put("devices", Map.of("device", "unrelated report"));
        reports.put("sensors", Map.of("sensor", "first report"));
        reports.put("./sensors", Map.of("sensor", "overwriting report"));

        IOException error = assertThrows(IOException.class,
                () -> writer.write(input, reportsDirectory, reports));

        assertTrue(error.getMessage().startsWith("Output files conflict:"), error::getMessage);
        assertFalse(Files.exists(reportsDirectory), "Reject aliases before creating the directory or any report");
        assertEquals("original input", Files.readString(input));
    }

    @Test
    void rejectsFreshAliasesThroughSymbolicParentsBeforeWriting() throws IOException
    {
        Path input = inputFile();
        Path resolvedParent = symbolicDotDotParent();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        reports.put("devices", Map.of());
        reports.put(resolvedParent.resolve("sensors").toString(), Map.of("value", "first"));
        reports.put("alias/../sensors", Map.of("value", "second"));

        IOException error = assertThrows(IOException.class,
                () -> writer.write(input, outputDirectory, reports));

        assertTrue(error.getMessage().startsWith("Output files conflict:"), error::getMessage);
        assertFalse(Files.exists(outputDirectory.resolve("devices.json")));
        assertFalse(Files.exists(resolvedParent.resolve("sensors.json")));
    }

    @Test
    void writesDistinctDestinationsThroughSymbolicParentAndDotDot() throws IOException
    {
        Path input = inputFile();
        Path resolvedParent = symbolicDotDotParent();
        Path distinctParent = Files.isSameFile(resolvedParent, outputDirectory)
                ? outputDirectory.resolve("real") : outputDirectory;
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        reports.put(distinctParent.resolve("sensors").toString(), Map.of("value", "first"));
        reports.put("alias/../sensors", Map.of("value", "second"));

        for (int attempt = 0; attempt < 2; attempt++)
        {
            writer.write(input, outputDirectory, reports);

            assertEquals("first", JsonParser.parseString(Files.readString(distinctParent.resolve("sensors.json")))
                    .getAsJsonObject().get("value").getAsString());
            assertEquals("second", JsonParser.parseString(Files.readString(resolvedParent.resolve("sensors.json")))
                    .getAsJsonObject().get("value").getAsString());
        }
    }

    private Path symbolicDotDotParent() throws IOException
    {
        Files.createDirectories(outputDirectory.resolve("real/sub"));
        Files.createSymbolicLink(outputDirectory.resolve("alias"), Path.of("real/sub"));
        // Probe actual file access: Windows and Unix resolve a symbolic parent followed by .. differently.
        Path probe = Files.createTempFile(outputDirectory.resolve("alias/.."), "parent-", ".tmp");
        try
        {
            return probe.toRealPath().getParent();
        }
        finally
        {
            Files.delete(probe);
        }
    }

    @Test
    void rejectsFreshReportAliasesWithoutDotDot() throws IOException
    {
        Path input = inputFile();
        Files.createDirectory(outputDirectory.resolve("real"));
        Files.createSymbolicLink(outputDirectory.resolve("alias"), Path.of("real"));
        IOException error = assertThrows(IOException.class, () -> writer.write(input, outputDirectory,
                Map.of("real/sensors", Map.of(), "alias/sensors", Map.of())));

        assertTrue(error.getMessage().startsWith("Output files conflict:"), error::getMessage);
        assertFalse(Files.exists(outputDirectory.resolve("real/sensors.json")));
    }
}
