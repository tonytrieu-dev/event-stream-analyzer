package com.esa.output;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
