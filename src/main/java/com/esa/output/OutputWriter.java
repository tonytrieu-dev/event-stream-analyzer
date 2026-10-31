package com.esa.output;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface OutputWriter
{
    void write(String fileBase, Map<String, Object> data, Path outputDirectory) throws IOException;
}
