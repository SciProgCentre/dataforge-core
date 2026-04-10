package space.kscience.tables.io

import kotlinx.io.Sink
import kotlinx.io.readByteArray
import kotlinx.io.writeString
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.io.Envelope
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.parseAsName
import space.kscience.tables.*
import kotlin.reflect.typeOf


/**
 * Finite or infinite [Rows] created from a fixed width text binary
 */
internal class TextRows(
    override val headers: ValueTableHeader,
    private val binary: Binary,
    private val delimiter: Regex,
) : Rows<Value> {

    override fun rowSequence(): Sequence<Row<Value>> = binary.read {
        val text = readByteArray().decodeToString()
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map {
                Table.readTextRow(it, headers, delimiter)
            }
//        flow {
//            do {
//                val line = readUTF8Line()
//                if (!line.isNullOrBlank()) {
//                    val row = readRow(headers, line)
//                    emit(row)
//                }
//            } while (!endOfInput)
//        }
    }

    companion object{

        fun readHeader(meta: Meta) = meta.getIndexed("header.column".parseAsName())
            .entries.sortedBy { it.key?.toInt() }
            .map { (_, item) ->
                SimpleColumnHeader<Value>(item["name"].string!!, typeOf<Value>(), item["meta"] ?: Meta.EMPTY)
            }
    }
}

/**
 * Read a line as a fixed width [Row]
 */
public fun Table.Companion.readTextRow(string: String, header: ValueTableHeader, delimiter: Regex): Row<Value> {
    val values = string.trim().split(delimiter).map { it.lazyParseValue() }

    if (values.size == header.size) {
        val map = header.map { it.name }.zip(values).toMap()
        return MapRow(map)
    } else {
        error("Can't read line \"${string}\". Expected ${header.size} values in a line, but found ${values.size}")
    }
}



/**
 * Write a fixed width value to the output
 */
private fun Sink.writeValue(value: Value, width: Int, left: Boolean = true) {
    require(width > 5) { "Width could not be less than 5" }
    val str: String = when (value.type) {
        ValueType.NUMBER -> value.numberOrNull.toString() //TODO apply decimal format
        ValueType.STRING, ValueType.LIST -> value.string.take(width)
        ValueType.BOOLEAN -> if (value.boolean) {
            "true"
        } else {
            "false"
        }
        ValueType.NULL -> "@null"
    }
    val padded = if (left) {
        str.padEnd(width)
    } else {
        str.padStart(width)
    }
    writeString(padded)
}

public val ColumnHeader<Value>.textWidth: Int
    get() = meta["columnWidth"].int ?: when (valueType) {
        ValueType.NUMBER -> 8
        ValueType.STRING -> 16
        ValueType.BOOLEAN -> 5
        ValueType.NULL -> 5
        ValueType.LIST -> 32
        null -> 16
    }

/**
 * Write TSV (or in more general case use [delimiter]) rows without header to the output.
 */
public fun Table.Companion.writeTextRows(sink: Sink, rows: Rows<Value>, delimiter: String = "\t") {
    val widths: List<Int> = rows.headers.map {
        it.textWidth
    }
    rows.rowSequence().forEach { row ->
        rows.headers.forEachIndexed { index, columnHeader ->
            sink.writeValue(row[columnHeader], widths[index])
            sink.writeString(delimiter)
        }
//        appendLine()
        sink.writeString("\r\n")
    }
}


/**
 * Read TSV rows from the given envelope
 */
public fun Table.Companion.readTextRows(envelope: Envelope, delimiter: Regex = "\\s+".toRegex()): Rows<Value> {
    val header = TextRows.readHeader(envelope.meta)
    return TextRows(header, envelope.data ?: Binary.EMPTY, delimiter)
}