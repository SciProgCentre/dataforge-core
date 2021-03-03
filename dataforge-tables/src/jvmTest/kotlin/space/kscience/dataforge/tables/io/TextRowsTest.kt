package space.kscience.dataforge.tables.io

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import space.kscience.dataforge.io.toByteArray
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.tables.Table
import space.kscience.dataforge.tables.row
import space.kscience.dataforge.values.Value
import space.kscience.dataforge.values.int
import space.kscience.dataforge.values.string
import kotlin.test.Test
import kotlin.test.assertEquals


@DFExperimental
class TextRowsTest {
    val table = Table<Value> {
        val a by column<Value>()
        val b by column<Value>()
        row(a to 1, b to "b1")
        row(a to 2, b to "b2")
    }

    @Test
    fun testTableWriteRead() = runBlocking {
        val envelope = table.toEnvelope()
        val string = envelope.data!!.toByteArray().decodeToString()
        println(string)
        val table = TextRows.readEnvelope(envelope)
        val rows = table.rowFlow().toList()
        assertEquals(1, rows[0]["a"]?.int)
        assertEquals("b2", rows[1]["b"]?.string)
    }
}