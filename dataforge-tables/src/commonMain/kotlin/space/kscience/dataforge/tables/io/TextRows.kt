package space.kscience.dataforge.tables.io

import io.ktor.utils.io.core.Output
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.flow.*
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.io.readSafeUtf8Line
import space.kscience.dataforge.io.writeUtf8String
import space.kscience.dataforge.tables.*
import space.kscience.dataforge.values.*

/**
 * Read a lin as a fixed width [Row]
 */
private fun readRow(header: ValueTableHeader, line: String): Row<Value> {
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
public class TextRows(override val headers: ValueTableHeader, private val binary: Binary) : Rows<Value> {

    /**
     * A flow of indexes of string start offsets ignoring empty strings
     */
    public fun indexFlow(): Flow<Int> = binary.read {
        //TODO replace by line reader
        val text = readBytes().decodeToString()
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

    override fun rowFlow(): Flow<Row<Value>> = binary.read {
        val text = readBytes().decodeToString()
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { readRow(headers, it) }.asFlow()
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

    public companion object
}

/**
 * Create a row offset index for [TextRows]
 */
public suspend fun TextRows.buildRowIndex(): List<Int> = indexFlow().toList()

/**
 * Finite table created from [RandomAccessBinary] with fixed width text table
 */
public class TextTable(
    override val headers: ValueTableHeader,
    private val binary: Binary,
    public val index: List<Int>,
) : Table<Value> {

    override val columns: Collection<Column<Value>> get() = headers.map { RowTableColumn(this, it) }

    override val rows: List<Row<Value>> get() = index.map { readAt(it) }

    override fun rowFlow(): Flow<Row<Value>> = TextRows(headers, binary).rowFlow()

    private fun readAt(offset: Int): Row<Value> {
        return binary.read(offset) {
            val line = readSafeUtf8Line()
            return@read readRow(headers, line)
        }
    }

    override fun get(row: Int, column: String): Value? {
        val offset = index[row]
        return readAt(offset)[column]
    }

    public companion object {
        public suspend operator fun invoke(header: ValueTableHeader, binary: Binary): TextTable {
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
    writeUtf8String(padded)
}

/**
 * Write rows without header to the output
 */
public suspend fun Output.writeRows(rows: Rows<Value>) {
    val widths: List<Int> = rows.headers.map {
        it.textWidth
    }
    rows.rowFlow().collect { row ->
        rows.headers.forEachIndexed { index, columnHeader ->
            writeValue(row[columnHeader] ?: Null, widths[index])
        }
//        appendLine()
        writeUtf8String("\r\n")
    }
}