# Event Stream Analyzer

Event Stream Analyzer processes IoT and smart building event records from a
plain-text file. It parses each line into key-value pairs, classifies valid
sensor readings, device events, and access records, and writes a separate JSON
report for each category. Invalid or unrecognized lines are skipped so processing
can continue through the rest of the file.

## Build and run

Install Java 17 or later and Maven. From the repository root, build the runnable
JAR and run the full test suite:

```sh
mvn clean verify
```

Run the included sample to write reports into `results/`:

```sh
java -jar target/analyzer.jar --file src/test/resources/sample/events.txt --outdir results
```

Replace the sample path with your own input file. Paths containing spaces must be
quoted.

| Argument | Required | Purpose |
|---|---|---|
| `--file <path>` | Yes | Input event file. |
| `--outdir <directory>` | No | Report directory; defaults to the current directory. |

The application creates the output directory if needed and writes `sensors.json`,
`devices.json`, and `access.json`, replacing existing reports. Reports use
two-space indentation. A category with no valid records produces exactly `{}`.

Invalid arguments and file errors print a message to stderr and return a nonzero
exit code. Before writing reports, the application rejects report paths that point to
the input file or to another report, including symbolic and hard links. It also
rejects symbolic links whose targets do not exist.

Maven manages the dependencies: Gson for JSON output and JUnit 5 for tests.

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
the implementations, while `AnalyzerPipeline` collects the reports and passes
the complete batch to `OutputWriter`. The writer owns report filenames and
preflights every destination before creating the output directory or changing a
report. This keeps future composite formats responsible for checking all of
their destinations together.

The [design report](docs/design.pdf) explains these decisions, their trade-offs,
and the changes needed for Network Logs, JSON Lines, and CSV. The UML source is
in [docs/class-diagram.puml](docs/class-diagram.puml), with
[PNG](docs/class-diagram.png) and [SVG](docs/class-diagram.svg) versions alongside it.
