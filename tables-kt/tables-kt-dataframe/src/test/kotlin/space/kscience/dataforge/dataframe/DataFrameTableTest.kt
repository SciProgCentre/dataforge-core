package space.kscience.dataforge.dataframe

import org.jetbrains.kotlinx.dataframe.api.add
import org.junit.jupiter.api.Test
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.tables.*
import kotlin.math.pow
import kotlin.reflect.typeOf
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

@OptIn(DFExperimental::class)
internal class DataFrameTableTest {

    @Test
    fun convertTableToDataFrame() {
        val x by ColumnHeader.typed<Double>()
        val x2 by ColumnHeader.typed<Double>()
        val y by ColumnHeader.typed<Double>()

        val table = ColumnTable<Double?>(100) {
            //filling column with double values equal to index
            fill(x) { it.toDouble() }
            //virtual column filled with x^2
            transform(x2) { it[x].pow(2) }
            //Fixed column filled with x^2 + 1
            column(y, x2.values.map { it?.plus(1) })
        }

        val dataFrame = table.toDataFrame()

        val newFrame = dataFrame.add("z") { it[x] + it[y] + 1.0 }

        val newTable = newFrame.asTable()

        assertEquals(table.columns[x], newTable.columns[x])
        assertTrue {
            table.rowsToColumn("z") { it[x] + it[y] + 1.0 }.contentEquals(newTable.columns["z"])
        }
    }

    @Test
    fun testDataFrameAccessors() {
        val x by ColumnHeader.typed<Double>()
        val y by ColumnHeader.typed<Number>()
        val y2 = SimpleColumnHeader<Int>("y", typeOf<Int>(), Meta.EMPTY)

        val table = ColumnTable<Number?>(100) {
            //filling column with double values equal to index
            fill(x) { it.toDouble() }
            fill(y) { it.toDouble() }
        }

        val dataFrame = table.toDataFrame()

        assertEquals(table[2, x], dataFrame[2][x])
        assertEquals(table[2, y], dataFrame[2][y])
        assertFails {
            dataFrame[2][y2]
        }
    }
}