package com.esa;

import com.esa.cli.ArgParser;
import com.esa.cli.CliException;
import com.esa.cli.CliOptions;
import java.nio.file.Files;

public final class Main {

    private static final int EXIT_USAGE_ERROR = 2;

    private Main() {
    }

    public static void main(String[] args) {
        CliOptions options;
        try {
            options = new ArgParser().parse(args);
        } catch (CliException e) {
            exitWithError(e.getMessage());
            return;
        }

        if (!Files.isRegularFile(options.inputFile())) {
            exitWithError("Input file does not exist: " + options.inputFile());
            return;
        }

        // TODO (Phase 2+): wire pipeline here (parse -> classify -> aggregate -> write).
    }

    private static void exitWithError(String message) {
        System.err.println("Error: " + message);
        System.exit(EXIT_USAGE_ERROR);
    }
}
