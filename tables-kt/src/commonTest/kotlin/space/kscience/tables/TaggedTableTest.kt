package space.kscience.tables

import kotlinx.coroutines.test.runTest
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.io.*
import space.kscience.dataforge.meta.Value
import space.kscience.dataforge.meta.ValueType
import space.kscience.dataforge.meta.int
import space.kscience.dataforge.meta.string
import space.kscience.tables.io.readTextTable
import space.kscience.tables.io.toTextEnvelope
import kotlin.test.Test
import kotlin.test.assertEquals

class TaggedTableTest {
    @Test
    fun testTaggedTableWriteRead() = runTest {
        val table = RowTable<Value> {
            val a by column(ValueType.NUMBER)
            val b by column(ValueType.STRING)
            valueRow(a to 1, b to "b1")
            valueRow(a to 2, b to "b2")
        }

        // 1. Convert table to Envelope
        val envelope = table.toTextEnvelope()

        // 2. Write Envelope to byte array using TaggedEnvelopeFormat
        val format = TaggedEnvelopeFormat(Global.io)
        val binary = Binary {
            format.writeTo(this, envelope)
        }
        val bytes = binary.toByteArray()

        println(bytes.decodeToString())

        // 3. Read Envelope back from the byte array
        val readEnvelope = format.readFrom(bytes.asBinary())

        // 4. Read Table back from Envelope
        val readTable = Table.readTextTable(readEnvelope)

        // 5. Assertions
        val rows = readTable.rowSequence().toList()
        assertEquals(2, rows.size)
        assertEquals(1, rows[0].getOrNull("a")?.int)
        assertEquals("b1", rows[0].getOrNull("b")?.string)
        assertEquals(2, rows[1].getOrNull("a")?.int)
        assertEquals("b2", rows[1].getOrNull("b")?.string)
    }
}