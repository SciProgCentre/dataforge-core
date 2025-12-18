package space.kscience.tables.io

import kotlinx.io.readByteArray
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.meta.Value
import space.kscience.dataforge.meta.lazyParseValue
import space.kscience.tables.MapRow
import space.kscience.tables.Row
import space.kscience.tables.Rows
import space.kscience.tables.ValueTableHeader

/**
 * Read a line as a fixed width [Row]
 */
internal fun String.readRow(header: ValueTableHeader, delimiter: Regex): Row<Value> {
    val values = trim().split(delimiter).map { it.lazyParseValue() }

    if (values.size == header.size) {
        val map = header.map { it.name }.zip(values).toMap()
        return MapRow(map)
    } else {
        error("Can't read line \"${this}\". Expected ${header.size} values in a line, but found ${values.size}")
    }
}

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
            .map { it.readRow(headers, delimiter) }
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

}