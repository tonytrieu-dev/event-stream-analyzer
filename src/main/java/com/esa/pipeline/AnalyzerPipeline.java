package com.esa.pipeline;

import com.esa.aggregate.Aggregator;
import com.esa.classify.RecordFactory;
import com.esa.model.Record;
import com.esa.model.RecordType;
import com.esa.output.OutputWriter;
import com.esa.parse.LineParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class AnalyzerPipeline
{
    private final LineParser parser;
    private final RecordFactory factory;
    private final Map<String, Aggregator> outputs;
    private final OutputWriter writer;
    private final Map<RecordType, Aggregator> aggregatorsByType = new EnumMap<>(RecordType.class);

    public AnalyzerPipeline(LineParser parser, RecordFactory factory,
            Map<String, Aggregator> outputs, OutputWriter writer)
    {
        this.parser = parser;
        this.factory = factory;
        this.outputs = outputs;
        this.writer = writer;
        for (Aggregator aggregator : outputs.values())
        {
            aggregatorsByType.put(aggregator.handles(), aggregator);
        }
    }

    public void run(Path inputFile, Path outputDirectory) throws IOException
    {
        try (BufferedReader input = Files.newBufferedReader(inputFile))
        {
            String line;
            while ((line = input.readLine()) != null)
            {
                Optional<Record> record = factory.classify(parser.parse(line));
                if (record.isEmpty())
                {
                    continue;
                }
                aggregatorsByType.get(record.get().type()).accept(record.get());
            }
        }

        for (Map.Entry<String, Aggregator> output : outputs.entrySet())
        {
            writer.write(output.getKey(), output.getValue().result(), outputDirectory);
        }
    }
}
