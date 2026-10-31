package com.esa.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.esa.aggregate.AccessAggregator;
import com.esa.aggregate.Aggregator;
import com.esa.aggregate.DeviceAggregator;
import com.esa.aggregate.SensorAggregator;
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

    @Test
    void createsNestedDirectoriesAndWritesTheRequestedFileWithItsContents() throws IOException
    {
        Path nestedDirectory = outputDirectory.resolve("reports/daily");

        writer.write("report", Map.of("message", "Heartbeat OK"), nestedDirectory);

        assertEquals("{\"message\":\"Heartbeat OK\"}",
                JsonParser.parseString(Files.readString(nestedDirectory.resolve("report.json"))).toString());
    }

    @Test
    void writesExactlyAnEmptyObjectForEachEmptyAggregator() throws IOException
    {
        Map<String, Aggregator> aggregators = Map.of(
                "sensors", new SensorAggregator(), "devices", new DeviceAggregator(),
                "access", new AccessAggregator());

        for (Map.Entry<String, Aggregator> entry : aggregators.entrySet())
        {
            writer.write(entry.getKey(), entry.getValue().result(), outputDirectory);
            assertEquals("{}", Files.readString(outputDirectory.resolve(entry.getKey() + ".json")),
                    entry.getKey() + ".json must contain exactly {} when empty");
        }
    }

    @Test
    void indentsNestedObjectsWithTwoSpaces() throws IOException
    {
        writer.write("sensors", Map.of("temp", Map.of("unit", "celsius")), outputDirectory);

        assertEquals("{\n  \"temp\": {\n    \"unit\": \"celsius\"\n  }\n}",
                Files.readString(outputDirectory.resolve("sensors.json")));
    }

    @Test
    void writesWholeNumbersWithoutDecimalPointsOrExponents() throws IOException
    {
        writer.write("sensors", Map.of("values", List.of(40.0, -20.0, -0.0, 40)), outputDirectory);

        assertEquals("[40,-20,0,40]", JsonParser.parseString(
                Files.readString(outputDirectory.resolve("sensors.json")))
                .getAsJsonObject().getAsJsonArray("values").toString());
    }

    @Test
    void roundsFractionalNumbersAndDropsDecimalsWhenTheResultIsWhole() throws IOException
    {
        writer.write("sensors", Map.of("values", List.of(21.64, 21.66, -2.36, 21.96)), outputDirectory);

        assertEquals("[21.6,21.7,-2.4,22]", JsonParser.parseString(
                Files.readString(outputDirectory.resolve("sensors.json")))
                .getAsJsonObject().getAsJsonArray("values").toString());
    }

    @Test
    void propagatesWriteFailuresAfterDirectoryCreationSucceeds() throws IOException
    {
        Files.createDirectory(outputDirectory.resolve("sensors.json"));

        assertThrows(IOException.class, () -> writer.write("sensors", Map.of(), outputDirectory),
                "writing to a path occupied by a directory must report the write failure");
    }
}
