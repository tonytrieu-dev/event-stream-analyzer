package com.esa.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ArgParserTest
{
    private final ArgParser parser = new ArgParser();

    @Test
    void defaultsOutputDirectoryToCurrentDirectoryWhenOutdirOmitted()
    {
        CliOptions parsedOptions = parser.parse(new String[] {"--file", "events.txt"});

        assertEquals(Path.of("events.txt"), parsedOptions.inputFile());
        assertEquals(Path.of("."), parsedOptions.outputDir(),
                "when --outdir is omitted, output dir must default to cwd (FR-22)");
    }

    @Test
    void usesTheProvidedOutputDirectoryWhenOutdirIsGiven()
    {
        CliOptions parsedOptions =
                parser.parse(new String[] {"--file", "events.txt", "--outdir", "results/"});

        assertEquals(Path.of("results/"), parsedOptions.outputDir(),
                "explicit --outdir must be used verbatim");
    }

    @Test
    void failsWhenRequiredFileFlagIsMissing()
    {
        CliException thrownException = assertThrows(CliException.class,
                () -> parser.parse(new String[] {"--outdir", "results/"}),
                "missing --file must fail fast, not default silently (FR-23)");

        assertTrue(thrownException.getMessage().toLowerCase().contains("--file"),
                "error message must name the missing --file flag to be actionable");
    }

    @Test
    void failsWhenFileFlagHasNoValue()
    {
        CliException thrownException = assertThrows(CliException.class,
                () -> parser.parse(new String[] {"--file"}),
                "a --file flag with no value is invalid, not an empty filename");

        assertTrue(thrownException.getMessage().toLowerCase().contains("--file"),
                "error message must name the offending flag");
    }
}
