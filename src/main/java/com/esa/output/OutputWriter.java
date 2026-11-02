package com.esa.output;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface OutputWriter
{
    void write(Path inputFile, Path outputDirectory, Map<String, Map<String, Object>> reports)
            throws IOException;
}
