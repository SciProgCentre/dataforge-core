package space.kscience.tables.csv

import com.jsoizo.kotlincsv.csvReader
import com.jsoizo.kotlincsv.csvWriter
import com.jsoizo.kotlincsv.reader.CsvReaderConfigBuilder
import com.jsoizo.kotlincsv.writer.CsvWriterConfigBuilder
import space.kscience.dataforge.meta.Meta
import space.kscience.tables.*
import kotlin.reflect.typeOf

internal fun List<String>.toHeader(): TableHeader<String> = map {
    SimpleColumnHeader(it, typeOf<String>(), Meta.EMPTY)
}

/**
 * Create a table from a CSV string. Use [headerOverride] for headers. If missing, use the first row as headers.
 */
public fun Table.Companion.readCsvString(
    string: String,
    headerOverride: List<String>? = null,
    format: CsvReaderConfigBuilder.() -> Unit = {},
): Table<String> {
    val data = csvReader(format).readAll(string)
    if (data.isEmpty()) error("Can't read empty table")
    val header = headerOverride ?: data.first()
    return RowTable(
        headers = header.toHeader(),
        data.let {
            //skip first line if it is header
            if (headerOverride == null) data.drop(1) else data
        }.map {
            MapRow(header.zip(it).toMap())
        }
    )
}

/**
 * Write a [Table] into a CSV string. Customize [toString] value conversion if necessary.
 */
public fun <T> Table.Companion.writeCsvString(
    table: Table<T?>,
    toString: (T?) -> String = { it.toString() },
    format: CsvWriterConfigBuilder.() -> Unit = {},
): String {
    val writer = csvWriter(format)
    val headerString = table.headers.joinToString(
        separator = writer.config.dialect.delimiter.toString(),
        postfix = writer.config.dialect.lineTerminator
    ) { it.name }
    return headerString + writer.writeAll(table.rows.map { row -> table.headers.map { toString(row.getOrNull(it.name)) } })
}
