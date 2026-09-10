# Module tables-kt-csv

CSV and TSV support for `tables-kt` using [`kotlin-csv`](https://github.com/doyaaaaaken/kotlin-csv).

## Methods

### Reading CSV

#### `Table.readCsvString` (Multiplatform)
Reads CSV data from a `String` into a `Table<String>`:
```kotlin
val table = Table.readCsvString(csvString)
```
- `headerOverride`: Optional custom column headers (`List<String>?`). If omitted/null, the first line of CSV is used as headers.
- `format`: Lambda to configure the CSV reader (`CsvReaderConfigBuilder.() -> Unit`), allowing customization of delimiter, quote character, charset, etc.

#### `Table.readCsv` (JVM - `Path`)
Reads a CSV file from a `java.nio.file.Path` into a `Table<String>`:
```kotlin
val table = Table.readCsv(Path.of("data.csv"))
```
- `headerOverride`: Optional custom column headers (`List<String>?`). If omitted/null, the first line is used as headers.
- `format`: `CsvReaderConfig` instance for reader configuration.

#### `Table.readCsvRows` (JVM - `Path`)
Reads a CSV file from a `java.nio.file.Path` as a lazy sequence of rows (`Rows<String>`):
```kotlin
val rows: Rows<String> = Table.readCsvRows(Path.of("data.csv"))
rows.rowSequence().forEach { row ->
    println(row["columnName"])
}
```
- Streams rows on-demand without loading the entire dataset into memory at once.
- `header`: Optional custom column headers (`List<String>?`).
- `format`: Lambda to configure the CSV reader (`CsvReaderConfigBuilder.() -> Unit`).

#### `Table.readCsv` (JVM - `URL`)
Reads CSV content directly from a `java.net.URL` into a `Table<String>`:
```kotlin
val table = Table.readCsv(URI("https://example.com/data.csv").toURL())
```
- `header`: Optional custom column headers (`List<String>?`).
- `format`: Lambda to configure the CSV reader (`CsvReaderConfigBuilder.() -> Unit`).

### Writing CSV

#### `Table.writeCsvString` (Multiplatform)
Writes a `Table` to a CSV-formatted `String`:
```kotlin
val csvString: String = Table.writeCsvString(table)
```
- `toString`: Converter function `(T?) -> String` for cell values (defaults to `{ it.toString() }`).
- `format`: Lambda to configure the CSV writer (`CsvWriterConfigBuilder.() -> Unit`), such as delimiter, dialect (e.g. `CsvDialect.TSV`), line terminator, quote character, etc.

#### `Table.writeCsvFile` (JVM - `Path`)
Writes a `Table` to a CSV file at a `java.nio.file.Path`:
```kotlin
Table.writeCsvFile(Path.of("output.csv"), table)
```
- `toString`: Converter function `(T?) -> String` for cell values (defaults to `{ it.toString() }`).
- `format`: Lambda to configure the CSV writer (`CsvWriterConfigBuilder.() -> Unit`).

## Usage

${artifact}
