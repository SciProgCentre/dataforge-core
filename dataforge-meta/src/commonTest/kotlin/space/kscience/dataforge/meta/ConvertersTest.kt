package space.kscience.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertEquals

class ConvertersTest {

    @Test
    fun stringListConversion() {
        val list = listOf("A", "B", "C")
        val meta = MetaConverter.stringList.convert(list)
        val json = meta.toJson()
        val reconstructedMeta = json.toMeta()
        val reconstructed = MetaConverter.stringList.read(reconstructedMeta)
        assertEquals(list,reconstructed)
    }
}