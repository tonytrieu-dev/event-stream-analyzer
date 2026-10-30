package com.esa;

import com.esa.cli.ArgParser;
import com.esa.cli.CliException;
import com.esa.cli.CliOptions;
import java.nio.file.Files;

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
        CliOptions options;
        try
        {
            options = new ArgParser().parse(args);
        }
        catch (CliException error)
        {
            return fail(error.getMessage());
        }

        if (!Files.isRegularFile(options.inputFile()))
        {
            return fail("Input file does not exist: " + options.inputFile());
        }

        // TODO (Phase 2+): wire pipeline here (parse -> classify -> aggregate -> write).
        return EXIT_SUCCESS;
    }

    private static int fail(String message)
    {
        System.err.println("Error: " + message);
        return EXIT_USAGE_ERROR;
    }
}
