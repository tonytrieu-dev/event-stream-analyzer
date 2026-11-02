package com.esa.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        writer.write(inputFile(), outputDirectory, Map.of("sensors", Map.of("values", List.of(40.0, -20.0, -0.0, 40))));

        assertEquals("[40,-20,0,40]", JsonParser.parseString(
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
    void checksAllDestinationsBeforeWriting() throws IOException
    {
        Path input = inputFile();
        Path sensors = outputDirectory.resolve("sensors.json");
        Path devices = outputDirectory.resolve("devices.json");
        Files.writeString(sensors, "existing sensors");
        Files.createLink(devices, sensors);

        assertThrows(IOException.class, () -> writer.write(input, outputDirectory,
                Map.of("sensors", Map.of(), "devices", Map.of())));
        assertEquals("existing sensors", Files.readString(sensors));
    }

}
