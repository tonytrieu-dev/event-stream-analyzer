package com.esa.cli;

import java.nio.file.Path;

// Throws instead of calling System.exit so the parser stays pure and testable;
// Main owns termination.
public class ArgParser
{
    private static final Path CURRENT_DIRECTORY = Path.of(".");

    public CliOptions parse(String[] args)
    {
        Path inputFile = null;
        Path outputDir = CURRENT_DIRECTORY;

        int index = 0;
        while (index < args.length)
        {
            String flag = args[index];
            switch (flag)
            {
                case "--file" ->
                {
                    inputFile = Path.of(requireValue(args, index, "--file"));
                    index += 2;
                }
                case "--outdir" ->
                {
                    outputDir = Path.of(requireValue(args, index, "--outdir"));
                    index += 2;
                }
                default -> throw new CliException("Unknown argument: " + flag);
            }
        }

        if (inputFile == null)
        {
            throw new CliException("Missing required argument: --file <filename.txt>");
        }
        return new CliOptions(inputFile, outputDir);
    }

    private static String requireValue(String[] args, int flagIndex, String flag)
    {
        int valueIndex = flagIndex + 1;
        if (valueIndex >= args.length)
        {
            throw new CliException("Missing value for argument: " + flag);
        }
        return args[valueIndex];
    }
}
