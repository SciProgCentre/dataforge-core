package space.kscience.tables

import space.kscience.dataforge.meta.Value
import space.kscience.dataforge.meta.ValueType
import space.kscience.dataforge.meta.int
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SpreadSheetTest {
    @Test
    fun spreadsheetWriteRead() {
        val a by ColumnHeader.value(ValueType.STRING)
        val b by ColumnHeader.value(ValueType.NUMBER)
        val c by ColumnHeader.value(ValueType.NUMBER)

        val ss = SpreadSheetTable<Value> {
            set(a, listOf("1", "2", "3"))
            set(b, listOf(1, 2, 3))

            set(2, c, 22)
        }

        assertEquals(22, ss[2, c]?.int)
        assertEquals(6, ss.columns["b"].sequence().sumOf { it?.int ?: 0 })
    }
}