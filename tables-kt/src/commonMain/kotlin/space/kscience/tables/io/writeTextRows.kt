package space.kscience.tables.io

import kotlinx.io.Sink
import kotlinx.io.writeString
import space.kscience.dataforge.meta.*
import space.kscience.tables.ColumnHeader
import space.kscience.tables.Rows
import space.kscience.tables.get
import space.kscience.tables.valueType

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
public fun Sink.writeTextRows(rows: Rows<Value>, delimiter: String = "\t") {
    val widths: List<Int> = rows.headers.map {
        it.textWidth
    }
    rows.rowSequence().forEach { row ->
        rows.headers.forEachIndexed { index, columnHeader ->
            writeValue(row[columnHeader], widths[index])
            writeString(delimiter)
        }
//        appendLine()
        writeString("\r\n")
    }
}