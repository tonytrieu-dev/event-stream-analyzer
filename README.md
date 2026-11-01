# Event Stream Analyzer

Event Stream Analyzer processes IoT and smart building event records from a
plain-text file. It parses each line into key-value pairs, classifies valid
sensor readings, device events, and access records, and writes a separate JSON
report for each category. Invalid or unrecognized lines are skipped so processing
can continue through the rest of the file.

## Build and run

The application requires Java 17 or later and Maven. Gson is the only third-party
library used by the application. Tests use JUnit 5.

```sh
mvn clean verify
java -jar target/analyzer.jar --file src/test/resources/sample/events.txt --outdir results
```

The `--file` argument is required. The `--outdir` argument is optional and defaults
to the current directory. Each run writes `sensors.json`, `devices.json`, and
`access.json`. A category with no records still produces a file containing exactly
`{}`. JSON reports use two-space indentation.

CLI and file errors produce a message on stderr and a nonzero exit code. Before
processing starts, the application checks that no report would overwrite the
input or share a file with another report. These checks include symbolic links
and hard links. Dangling report symlinks are rejected before any reports are
written.

## Statistical methods

The calculations follow these rules so the results can be reproduced:

- Sensor median uses the middle value in a sorted list with an odd count. For an
  even count, it averages the two middle values.
- Sensor standard deviation uses the population formula, dividing by N.
- Access percentiles use the nearest-rank method. For a sorted list of N values,
  the zero-based index is `ceil(p / 100 * N) - 1`. The 50th percentile and median
  can therefore differ for even-sized lists. This difference is intentional.
- Decimal outputs are rounded to one decimal place. Whole results are written
  without a decimal point.
- Device severity ties use CRITICAL > ERROR > WARN > INFO.
- A user is flagged after at least three denials on the same door.

## Verification

`mvn test` runs the unit and component tests. `mvn verify` also builds the runnable
JAR and tests it in separate processes. The CLI tests check the sample reports,
default and explicit output directories, error messages, exit codes, and file
collision checks. All file-based tests use temporary directories.

The assignment's 14 input lines are in `src/test/resources/sample/events.txt`.
The adjacent JSON files contain independently calculated expected results. The
assignment PDF's illustrative JSON describes different data, so the sample tests
use the results calculated from the actual input.

## Design

Parsing, classification, aggregation, and output have separate responsibilities.
`LineParser`, `Aggregator`, and `OutputWriter` provide the Strategy interfaces.
`RecordFactory` handles classification through a Simple Factory. `Main` selects
the implementations and output names, then passes them to `AnalyzerPipeline`.

The [design report](docs/design.pdf) explains these decisions, their trade-offs,
and the changes needed for Network Logs, JSON Lines, and CSV. The UML source is
in [docs/class-diagram.puml](docs/class-diagram.puml), with
[PNG](docs/class-diagram.png) and [SVG](docs/class-diagram.svg) versions alongside it.
