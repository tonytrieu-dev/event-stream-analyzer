package com.esa.output;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
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
    public void write(Path inputFile, Path outputDirectory, Map<String, Map<String, Object>> reports)
            throws IOException
    {
        List<Path> outputFiles = outputFiles(outputDirectory, reports);
        validateDestinations(inputFile, outputFiles);
        Files.createDirectories(outputDirectory);
        int index = 0;
        for (Map<String, Object> data : reports.values())
        {
            Files.writeString(outputFiles.get(index++), gson.toJson(data));
        }
    }

    private static List<Path> outputFiles(Path outputDirectory, Map<String, Map<String, Object>> reports)
    {
        List<Path> outputFiles = new ArrayList<>();
        for (String fileBase : reports.keySet())
        {
            outputFiles.add(outputDirectory.resolve(fileBase + ".json"));
        }
        return outputFiles;
    }

    private static void validateDestinations(Path inputFile, List<Path> outputFiles) throws IOException
    {
        for (int index = 0; index < outputFiles.size(); index++)
        {
            Path output = outputFiles.get(index);
            if (Files.isSymbolicLink(output) && !Files.exists(output))
            {
                throw new IOException("Output is a dangling symbolic link: " + output);
            }
            if (Files.exists(output) && Files.isSameFile(inputFile, output))
            {
                throw new IOException("Input file conflicts with output: " + output);
            }
            for (int previous = 0; previous < index; previous++)
            {
                Path checked = outputFiles.get(previous);
                if (Files.exists(output) && Files.exists(checked) && Files.isSameFile(output, checked))
                {
                    throw new IOException("Output files conflict: " + output + " and " + checked);
                }
            }
        }
    }

    private static BigDecimal formatNumber(Number number)
    {
        BigDecimal rounded = new BigDecimal(number.toString())
                .setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return rounded.setScale(Math.max(0, rounded.scale()));
    }
}
