package space.kscience.tables.io

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.io.readByteArray
import kotlinx.io.readLine
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.io.Envelope
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.NameToken
import space.kscience.tables.*

/**
 * Finite table created from [Binary] with fixed width text table
 */
internal class TextTable(
    override val headers: ValueTableHeader,
    private val binary: Binary,
    val index: List<Int>,
    val delimiter: Regex = "\\s+".toRegex(),
) : Table<Value> {

    override val columns: Collection<Column<Value>> get() = headers.map { RowTableColumn(this, it) }

    override val rows: List<Row<Value>> get() = index.map { readAt(it) }

    override fun rowSequence(): Sequence<Row<Value>> = TextRows(headers, binary, delimiter).rowSequence()

    private fun readAt(offset: Int): Row<Value> = binary.read(offset) {
        val line = readLine() ?: error("Line not found")
        Table.readTextRow(line, headers, delimiter)
    }

    override fun getOrNull(row: Int, column: String): Value? {
        val offset = index[row]
        return readAt(offset).getOrNull(column)
    }
}


/**
 * A flow of indexes of string start offsets ignoring empty strings
 */
private fun Binary.lineIndexFlow(): Flow<Int> = read {
    //TODO replace by line reader
    val text = readByteArray().decodeToString()
    text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .scan(0) { acc, str -> acc + str.length }.asFlow()
//        var counter: Int = 0
//        flow {
//            do {
//                val line = readUTF8Line()
//                counter += line?.length ?: 0
//                if (!line.isNullOrBlank()) {
//                    emit(counter)
//                }
//            } while (!endOfInput)
//        }
}


/**
 * Create a row offset index for [TextRows]
 */
private suspend fun Binary.buildRowIndex(): List<Int> = lineIndexFlow().toList()


/**
 * Convert given [Table] to a TSV-based envelope, encoding header in Meta
 */
public fun Table<Value>.toTextEnvelope(): Envelope = Envelope {
    meta {
        "header" put {
            headers.forEachIndexed { index: Int, columnHeader: ColumnHeader<Value> ->
                set(NameToken("column", index.toString()), Meta {
                    "name" put columnHeader.name
                    if (!columnHeader.meta.isEmpty()) {
                        "meta" put columnHeader.meta
                    }
                })
            }
        }
    }

    type = "table.value"
    dataID = "valueTable[${this@toTextEnvelope.hashCode()}]"

    data = Binary {
        Table.writeTextRows(this, this@toTextEnvelope)
    }
}

/**
 * Read given binary as TSV [Value] table.
 * This method does not read the whole table into memory. Instead, it reads it ones and saves line offset index. Then
 * it reads specific lines on-demand.
 */
public suspend fun Table.Companion.readTextTable(binary: Binary, header: ValueTableHeader): Table<Value> {
    val index = binary.buildRowIndex()
    return TextTable(header, binary, index)
}

/**
 * Read given [Envelope] as TSV [Value] table.
 */
public suspend fun Table.Companion.readTextTable(envelope: Envelope): Table<Value> {
    val header = TextRows.readHeader(envelope.meta)

    return readTextTable(envelope.data ?: Binary.EMPTY, header)
}


