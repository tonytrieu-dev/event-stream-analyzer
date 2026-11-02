package com.esa.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        Path sensors = Files.writeString(outputDirectory.resolve("sensors.json"), "existing sensors");
        Files.createLink(outputDirectory.resolve("devices.json"), sensors);
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        reports.put("sensors", Map.of("sensor", "first report"));
        reports.put("devices", Map.of("device", "overwriting report"));

        IOException error = assertThrows(IOException.class,
                () -> writer.write(input, outputDirectory, reports));

        assertTrue(error.getMessage().startsWith("Output files conflict:"), error::getMessage);
        assertEquals("existing sensors", Files.readString(sensors), "reject before changing any report");
        assertEquals("original input", Files.readString(input));
    }
}
