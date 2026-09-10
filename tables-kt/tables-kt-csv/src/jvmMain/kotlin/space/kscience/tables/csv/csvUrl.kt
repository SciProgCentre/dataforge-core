package space.kscience.tables.csv

import com.jsoizo.kotlincsv.reader.CsvReaderConfigBuilder
import space.kscience.tables.Table
import java.net.URL

/**
 * Read CSV from [url] as a table. Use [header] for headers. If missing, use the first row as headers.
 */
public fun Table.Companion.readCsv(
    url: URL,
    header: List<String>? = null,
    format: CsvReaderConfigBuilder.() -> Unit = {},
): Table<String> = readCsvString(url.readText(), header, format = format)

