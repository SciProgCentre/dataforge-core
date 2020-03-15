package hep.dataforge.tables.io

import hep.dataforge.tables.*
import hep.dataforge.values.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.io.Binary
import kotlinx.io.ExperimentalIoApi
import kotlinx.io.Output
import kotlinx.io.RandomAccessBinary
import kotlinx.io.text.forEachUtf8Line
import kotlinx.io.text.readUtf8Line
import kotlinx.io.text.readUtf8StringUntilDelimiter
import kotlinx.io.text.writeUtf8String
import kotlin.reflect.KClass

/**
 * Read a lin as a fixed width [Row]
 */
private fun readLine(header: ValueTableHeader, line: String): Row<Value> {
    val values = line.trim().split("\\s+".toRegex()).map { it.lazyParseValue() }

    if (values.size == header.size) {
        val map = header.map { it.name }.zip(values).toMap()
        return MapRow(map)
    } else {
        error("Can't read line \"$line\". Expected ${header.size} values in a line, but found ${values.size}")
    }
}

/**
 * Finite or infinite [Rows] created from a fixed width text binary
 */
@ExperimentalIoApi
class TextRows(override val header: ValueTableHeader, val binary: Binary) : Rows<Value> {

    /**
     * A flow of indexes of string start offsets ignoring empty strings
     */
    fun indexFlow(): Flow<Int> = binary.read {
        var counter: Int = 0
        flow {
            val string = readUtf8StringUntilDelimiter('\n')
            counter += string.length
            if (!string.isBlank()) {
                emit(counter)
            }
        }
    }

    override fun rowFlow(): Flow<Row<Value>> = binary.read {
        flow {
            forEachUtf8Line { line ->
                if (line.isNotBlank()) {
                    val row = readLine(header, line)
                    emit(row)
                }
            }
        }
    }

    companion object
}

/**
 * Create a row offset index for [TextRows]
 */
@ExperimentalIoApi
suspend fun TextRows.buildRowIndex(): List<Int> = indexFlow().toList()

/**
 * Finite table created from [RandomAccessBinary] with fixed width text table
 */
@ExperimentalIoApi
class TextTable(
    override val header: ValueTableHeader,
    val binary: RandomAccessBinary,
    val index: List<Int>
) : Table<Value> {

    override val columns: Collection<Column<Value>> get() = header.map { RowTableColumn(this, it) }

    override val rows: List<Row<Value>> get() = index.map { readAt(it) }

    override fun rowFlow(): Flow<Row<Value>> = TextRows(header, binary).rowFlow()

    private fun readAt(offset: Int): Row<Value> {
        return binary.read(offset) {
            val line = readUtf8Line()
            return@read readLine(header, line)
        }
    }

    override fun <T : Value> getValue(row: Int, column: String, type: KClass<out T>): T? {
        val offset = index[row]
        return type.cast(readAt(offset)[column])
    }

    companion object {
        suspend operator fun invoke(header: ValueTableHeader, binary: RandomAccessBinary): TextTable {
            val index = TextRows(header, binary).buildRowIndex()
            return TextTable(header, binary, index)
        }
    }
}


/**
 * Write a fixed width value to the output
 */
private fun Output.writeValue(value: Value, width: Int, left: Boolean = true) {
    require(width > 5) { "Width could not be less than 5" }
    val str: String = when (value.type) {
        ValueType.NUMBER -> value.number.toString() //TODO apply decimal format
        ValueType.STRING -> value.string.take(width)
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
    writeUtf8String(padded)
}

/**
 * Write rows without header to the output
 */
suspend fun Output.writeRows(rows: Rows<Value>) {
    val widths: List<Int> = rows.header.map {
        it.textWidth
    }
    rows.rowFlow().collect { row ->
        rows.header.forEachIndexed { index, columnHeader ->
            writeValue(row[columnHeader] ?: Null, widths[index])
        }
        writeUtf8String("\r\n")
    }
}