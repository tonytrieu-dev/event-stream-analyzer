package com.esa.output;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class JsonWriter implements OutputWriter
{
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeHierarchyAdapter(Number.class,
                    (JsonSerializer<Number>) (number, type, context) -> new JsonPrimitive(formatNumber(number)))
            .create();

    @Override
    public void write(String fileBase, Map<String, Object> data, Path outputDirectory) throws IOException
    {
        Files.createDirectories(outputDirectory);
        Files.writeString(outputDirectory.resolve(fileBase + ".json"), gson.toJson(data));
    }

    private static BigDecimal formatNumber(Number number)
    {
        BigDecimal rounded = new BigDecimal(number.toString())
                .setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return rounded.setScale(Math.max(0, rounded.scale()));
    }
}
