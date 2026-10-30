package com.esa.cli;

import java.nio.file.Path;

public record CliOptions(Path inputFile, Path outputDir) {
}
