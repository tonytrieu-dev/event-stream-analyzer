package com.esa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest
{
    @Test
    void returnsNonZeroWhenInputFileDoesNotExist()
    {
        int exitCode = Main.run(new String[] {"--file", "no-such-file.txt"});

        assertNotEquals(0, exitCode, "a nonexistent input file must exit non-zero (FR-23)");
    }

    @Test
    void returnsZeroForAnExistingInputFile(@TempDir Path tempDir) throws IOException
    {
        Path existingFile = Files.createFile(tempDir.resolve("events.txt"));

        int exitCode = Main.run(new String[] {"--file", existingFile.toString()});

        assertEquals(0, exitCode, "a valid, existing input file must exit zero");
    }
}
