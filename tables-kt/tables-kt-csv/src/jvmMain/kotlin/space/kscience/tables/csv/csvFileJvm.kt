package space.kscience.tables.csv

import com.jsoizo.kotlincsv.csvReader
import com.jsoizo.kotlincsv.csvWriter
import com.jsoizo.kotlincsv.reader.*
import com.jsoizo.kotlincsv.writer.CsvWriterConfigBuilder
import com.jsoizo.kotlincsv.writer.write
import space.kscience.tables.*
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

/**
 * Read a CSV file into a table. Use [headerOverride] for headers. If missing, use the first row as headers.
 */
public fun Table.Companion.readCsv(
    path: Path,
    headerOverride: List<String>? = null,
    format: CsvReaderConfig = CsvReaderConfig(),
): Table<String> {
    path.inputStream().use { inputStream ->
        val data = csvReader(format).readAll(inputStream)
        if (data.isEmpty()) error("Can't read empty table")
        val header = headerOverride ?: data.first()
        return RowTable(
            headers = header.toHeader(),
            data.map {
                MapRow(header.zip(it).toMap())
            }
        )
    }
}

/**
 * Read a CSV file into a sequence of rows. Use [header] for headers. If missing, use the first row as headers.
 */
public fun Table.Companion.readCsvRows(
    path: Path,
    header: List<String>? = null,
    format: CsvReaderConfigBuilder.() -> Unit = {},
): Rows<String> = path.inputStream().use { inputStream ->
    csvReader(format).read(inputStream) { sequence ->
        val header = header ?: sequence.take(1).first()
        object : Rows<String> {
            override val headers: TableHeader<String> get() = header.toHeader()

            override fun rowSequence(): Sequence<Row<String>> = sequence.withHeader().map {
                MapRow(it)
            }

        }
    }
}

/**
 * Write a [Table] into a csv file in [path]. Customize [toString] value conversion if necessary.
 */
public fun <T> Table.Companion.writeCsvFile(
    path: Path,
    table: Table<T>,
    toString: (T?) -> String = { it.toString() },
    format: CsvWriterConfigBuilder.() -> Unit = {},
) {
    val writer = csvWriter(format)
    path.outputStream().use { outputStream ->
        val headerString = table.headers.joinToString(
            separator = writer.config.dialect.delimiter.toString(),
            postfix = writer.config.dialect.lineTerminator
        ) { it.name }
        outputStream.write(headerString.encodeToByteArray())
        writer.write(table.rowSequence().map { row: Row<T> -> table.headers.map { toString(row.getOrNull(it.name)) } }, outputStream)
    }
}