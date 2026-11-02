package com.esa;

import com.esa.aggregate.AccessAggregator;
import com.esa.aggregate.Aggregator;
import com.esa.aggregate.DeviceAggregator;
import com.esa.aggregate.SensorAggregator;
import com.esa.classify.RecordFactory;
import com.esa.cli.ArgParser;
import com.esa.cli.CliException;
import com.esa.cli.CliOptions;
import com.esa.output.JsonWriter;
import com.esa.parse.KeyValueParser;
import com.esa.pipeline.AnalyzerPipeline;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;

public final class Main
{
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_USAGE_ERROR = 2;

    private Main()
    {
    }

    public static void main(String[] args)
    {
        System.exit(run(args));
    }

    static int run(String[] args)
    {
        try
        {
            CliOptions options = new ArgParser().parse(args);
            if (!Files.isRegularFile(options.inputFile()))
            {
                return fail("Input file does not exist: " + options.inputFile());
            }

            Map<String, Aggregator> outputs = Map.of("sensors", new SensorAggregator(),
                    "devices", new DeviceAggregator(), "access", new AccessAggregator());
            AnalyzerPipeline pipeline = new AnalyzerPipeline(new KeyValueParser(), new RecordFactory(),
                    outputs, new JsonWriter());
            pipeline.run(options.inputFile(), options.outputDir());
            return EXIT_SUCCESS;
        }
        catch (CliException | InvalidPathException | IOException error)
        {
            return fail(error.getMessage());
        }
    }

    private static int fail(String message)
    {
        System.err.println("Error: " + message);
        return EXIT_USAGE_ERROR;
    }
}
