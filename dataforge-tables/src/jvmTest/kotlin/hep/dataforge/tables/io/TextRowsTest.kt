package hep.dataforge.tables.io

import hep.dataforge.misc.DFExperimental
import hep.dataforge.tables.Table
import hep.dataforge.tables.get
import hep.dataforge.tables.row
import hep.dataforge.values.Value
import hep.dataforge.values.int
import hep.dataforge.values.string
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.io.ExperimentalIoApi
import kotlinx.io.toByteArray
import kotlin.test.Test
import kotlin.test.assertEquals


@DFExperimental
@ExperimentalIoApi
class TextRowsTest {
    val table = Table<Value> {
        val a = column<Value>("a")
        val b = column<Value>("b")
        row(a to 1, b to "b1")
        row(a to 2, b to "b2")
    }

    @Test
    fun testTableWriteRead() {
        runBlocking {
            val envelope = table.wrap()
            val string = envelope.data!!.toByteArray().decodeToString()
            println(string)
            val table = TextRows.readEnvelope(envelope)
            val rows = table.rowFlow().toList()
            assertEquals(1, rows[0]["a"]?.int)
            assertEquals("b2", rows[1]["b"]?.string)
        }
    }
}