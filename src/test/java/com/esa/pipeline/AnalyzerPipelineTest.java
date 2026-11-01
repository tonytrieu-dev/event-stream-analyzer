package com.esa.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.esa.aggregate.AccessAggregator;
import com.esa.aggregate.DeviceAggregator;
import com.esa.aggregate.SensorAggregator;
import com.esa.classify.RecordFactory;
import com.esa.output.JsonWriter;
import com.esa.parse.KeyValueParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnalyzerPipelineTest
{
    @TempDir
    Path temporaryDirectory;

    private final AnalyzerPipeline pipeline = new AnalyzerPipeline(new KeyValueParser(), new RecordFactory(),
            Map.of("sensors", new SensorAggregator(), "devices", new DeviceAggregator(),
                    "access", new AccessAggregator()), new JsonWriter());

    @Test
    void createsAllThreeEmptyReportsForEmptyInput() throws IOException
    {
        Path inputFile = Files.createFile(temporaryDirectory.resolve("empty.txt"));
        Path outputDirectory = temporaryDirectory.resolve("reports");

        pipeline.run(inputFile, outputDirectory);

        for (String filename : new String[] {"sensors.json", "devices.json", "access.json"})
        {
            assertEquals("{}", Files.readString(outputDirectory.resolve(filename)), filename);
        }
    }

}
