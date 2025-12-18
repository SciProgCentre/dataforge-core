package space.kscience.tables.csv

import com.github.doyaaaaaken.kotlincsv.dsl.context.CsvReaderContext
import com.github.doyaaaaaken.kotlincsv.dsl.context.CsvWriterContext
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import space.kscience.tables.*
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

public fun Table.Companion.readCsv(
    path: Path,
    format: CsvReaderContext.() -> Unit = {},
): Table<String> {
    path.inputStream().use { inputStream ->
        val data = csvReader(format).readAllWithHeader(inputStream)
        if (data.isEmpty()) error("Can't read empty table")
        return RowTable(
            headers = data.first().extractHeader(),
            data.map { MapRow(it) }
        )
    }
}

public fun Table.Companion.readCsvRows(
    path: Path,
    format: CsvReaderContext.() -> Unit = {},
): Rows<String> {
    path.inputStream().use { inputStream ->
        val sequence = csvReader(format).open(inputStream) {
            readAllWithHeaderAsSequence()
        }
        val firstRow = sequence.take(1).first()
        val header: List<ColumnHeader<String>> = firstRow.extractHeader()
        return object : Rows<String> {
            override val headers: TableHeader<String> get() = header

            override fun rowSequence(): Sequence<Row<String>> = sequence {
                yield(MapRow(firstRow))
                yieldAll(sequence.map { MapRow(it) })
            }

        }
    }
}

public fun Table.Companion.writeCsvFile(
    path: Path,
    table: Table<Any?>,
    format: CsvWriterContext.() -> Unit = {},
) {
    val writer = csvWriter(format)
    path.outputStream().use { outputStream ->
        val headerString = table.headers.joinToString(
            separator = writer.delimiter.toString(),
            postfix = writer.lineTerminator
        ) { it.name }
        outputStream.write(headerString.encodeToByteArray())
        writer.writeAll(table.rows.map { row -> table.headers.map { row[it] } }, outputStream)
    }
}