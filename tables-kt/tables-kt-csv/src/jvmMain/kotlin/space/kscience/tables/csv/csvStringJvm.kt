package space.kscience.tables.csv

import com.github.doyaaaaaken.kotlincsv.dsl.context.CsvReaderContext
import com.github.doyaaaaaken.kotlincsv.dsl.context.CsvWriterContext
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import space.kscience.tables.Table
import space.kscience.tables.get
import java.net.URL


public fun Table.Companion.readCsv(
    url: URL,
    format: CsvReaderContext.() -> Unit = {},
): Table<String> = readCsvString(url.readText(), format)

public fun Table.Companion.writeCsvString(
    table: Table<Any?>,
    format: CsvWriterContext.() -> Unit = {},
): String {
    val writer = csvWriter(format)
    val headerString = table.headers.joinToString(
        separator = writer.delimiter.toString(),
        postfix = writer.lineTerminator
    ) { it.name }
    return headerString + writer.writeAllAsString(table.rows.map { row -> table.headers.map { row[it] } })
}
