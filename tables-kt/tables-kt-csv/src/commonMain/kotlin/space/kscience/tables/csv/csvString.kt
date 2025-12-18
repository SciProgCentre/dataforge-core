package space.kscience.tables.csv

import com.github.doyaaaaaken.kotlincsv.dsl.context.CsvReaderContext
import com.github.doyaaaaaken.kotlincsv.dsl.context.CsvWriterContext
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import space.kscience.dataforge.meta.Meta
import space.kscience.tables.*
import kotlin.reflect.typeOf

internal fun Map<String, String>.extractHeader(): TableHeader<String> = keys.map {
    SimpleColumnHeader(it, typeOf<String>(), Meta.EMPTY)
}

public object CsvFormats {
    public val tsvReader: CsvReaderContext.() -> Unit = {
        quoteChar = '"'
        delimiter = '\t'
        escapeChar = '\\'
    }

    public val tsvWriter: CsvWriterContext.() -> Unit = {
        delimiter = '\t'
    }
}


public fun Table.Companion.readCsvString(
    string: String,
    format: CsvReaderContext.() -> Unit = {},
): Table<String> {
    val data = csvReader(format).readAllWithHeader(string)
    if (data.isEmpty()) error("Can't read empty table")
    return RowTable(
        headers = data.first().extractHeader(),
        data.map { MapRow(it) }
    )
}