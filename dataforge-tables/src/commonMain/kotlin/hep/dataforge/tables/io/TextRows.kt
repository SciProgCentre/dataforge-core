package hep.dataforge.tables.io

import hep.dataforge.meta.get
import hep.dataforge.meta.int
import hep.dataforge.meta.string
import hep.dataforge.tables.*
import hep.dataforge.values.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.io.Binary
import kotlinx.io.ExperimentalIoApi
import kotlinx.io.Output
import kotlinx.io.RandomAccessBinary
import kotlinx.io.text.forEachUtf8Line
import kotlinx.io.text.readUtf8Line
import kotlinx.io.text.writeUtf8String
import kotlin.reflect.KClass

private fun readLine(header: List<ColumnHeader<Value>>, line: String): Row {
    val values = line.split("\\s+".toRegex()).map { it.parseValue() }

    if (values.size == header.size) {
        val map = header.map { it.name }.zip(values).toMap()
        return MapRow(map)
    } else {
        error("Can't read line $line. Expected ${header.size} values in a line, but found ${values.size}")
    }
}


@ExperimentalIoApi
class TextRows(override val header: List<ColumnHeader<Value>>, val binary: Binary) : Rows {

    override fun rowFlow(): Flow<Row> = binary.read {
        flow {
            forEachUtf8Line { line ->
                if (line.isNotBlank()) {
                    val row = readLine(header, line)
                    emit(row)
                }
            }
        }
    }
}

@ExperimentalIoApi
class TextTable(
    override val header: List<ColumnHeader<Value>>,
    val binary: RandomAccessBinary,
    val index: List<Int>
) : Table<Value> {

    override val columns: Collection<Column<Value>> get() = header.map { RowTableColumn(this, it) }

    override val rows: List<Row> get() = index.map { readAt(it) }

    override fun rowFlow(): Flow<Row> = TextRows(header, binary).rowFlow()

    private fun readAt(offset: Int): Row {
        return binary.read(offset) {
            val line = readUtf8Line()
            return@read readLine(header, line)
        }
    }

    override fun <T : Any> getValue(row: Int, column: String, type: KClass<out T>): T? {
        val offset = index[row]
        return type.cast(readAt(offset)[column])
    }
}

fun Output.writeValue(value: Value, width: Int, left: Boolean = true) {
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

val ColumnHeader<Value>.valueType: ValueType? get() = meta["valueType"].string?.let { ValueType.valueOf(it) }

private val ColumnHeader<Value>.width: Int
    get() = meta["columnWidth"].int ?: when (valueType) {
        ValueType.NUMBER -> 8
        ValueType.STRING -> 16
        ValueType.BOOLEAN -> 5
        ValueType.NULL -> 5
        null -> 16
    }


/**
 * Write rows without header to the output
 */
suspend fun Output.writeRows(rows: Rows) {
    @Suppress("UNCHECKED_CAST") val header = rows.header.map {
        if (it.type != Value::class) error("Expected Value column, but found ${it.type}") else (it as ColumnHeader<Value>)
    }
    val widths: List<Int> = header.map {
        it.width
    }
    rows.rowFlow().collect { row ->
        header.forEachIndexed { index, columnHeader ->
            writeValue(row[columnHeader] ?: Null, widths[index])
        }
    }
}