package space.kscience.dataforge.meta

import kotlinx.serialization.json.Json
import space.kscience.dataforge.values.string
import kotlin.test.Test
import kotlin.test.assertEquals

class MetaSerializationTest {

    @Test
    fun singleValueDeserialization(){
        val string = "ddd"
        val meta = Json.decodeFromString(MetaSerializer, string)
        assertEquals(string, meta.value?.string)
    }
}