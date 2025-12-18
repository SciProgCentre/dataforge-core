package space.kscience.tables

import kotlin.test.Test
import kotlin.test.assertTrue

class ColumnTableTest {
    @Test
    fun columnBuilder() {
        val columnTable = ColumnTable<Double>(100) {
            val a by ColumnHeader.typed<Double>()
            val b by ColumnHeader.typed<Double>()

            // fill column with a new value
            fill(a) { it.toDouble() }
            // set column with pre-filled values
            column(b, List(100) { it.toDouble() })
            // add a virtual column with values transformed from rows
            transform("c") { it[a] - it[b] }
        }
        assertTrue {
            columnTable.columns["c"].listValues().all { it == 0.0 }
        }
    }
}