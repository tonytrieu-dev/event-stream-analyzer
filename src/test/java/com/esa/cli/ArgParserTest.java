package com.esa.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Phase 1 CLI tests (plan section 7: T-X1..X4).
 *
 * <p>Each test targets one distinct root cause, so a red test names the one
 * broken thing:
 * <ul>
 *   <li>T-X1: default-output-dir logic (--outdir omitted)
 *   <li>T-X2: output-dir override logic (--outdir honored)
 *   <li>T-X3: required-argument validation (--file missing)
 *   <li>T-X4: argument-value validation (--file present but empty)
 * </ul>
 *
 * <p>ArgParser is pure: it returns {@link CliOptions} or throws
 * {@link CliException}. The filesystem-existence check (the other half of
 * FR-23) lives in Main, which owns process termination; keeping it out of the
 * parser is what makes the parser unit-testable without touching disk.
 */
class ArgParserTest {

    private final ArgParser parser = new ArgParser();

    @Test
    void tX1_outdirOmitted_defaultsToCurrentDirectory() {
        CliOptions options = parser.parse(new String[] {"--file", "events.txt"});

        assertEquals(Path.of("events.txt"), options.inputFile());
        assertEquals(Path.of("."), options.outputDir(),
                "when --outdir is omitted, output dir must default to cwd (FR-22)");
    }

    @Test
    void tX2_outdirProvided_isHonored() {
        CliOptions options =
                parser.parse(new String[] {"--file", "events.txt", "--outdir", "results/"});

        assertEquals(Path.of("results/"), options.outputDir(),
                "explicit --outdir must be used verbatim");
    }

    @Test
    void tX3_fileFlagMissing_throwsCliException() {
        CliException thrown = assertThrows(CliException.class,
                () -> parser.parse(new String[] {"--outdir", "results/"}),
                "missing --file must fail fast, not default silently (FR-23)");

        assertTrue(thrown.getMessage().toLowerCase().contains("--file"),
                "error message must name the missing --file flag to be actionable");
    }

    @Test
    void tX4_fileFlagWithoutValue_throwsCliException() {
        CliException thrown = assertThrows(CliException.class,
                () -> parser.parse(new String[] {"--file"}),
                "a --file flag with no value is invalid, not an empty filename");

        assertTrue(thrown.getMessage().toLowerCase().contains("--file"),
                "error message must name the offending flag");
    }
}
