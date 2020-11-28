package hep.dataforge.names

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals


class NameSerializationTest {

    @Test
    fun testNameSerialization() {
        val name = "aaa.bbb.ccc".toName()
        val json = Json.encodeToJsonElement(Name.serializer(), name)
        println(json)
        val reconstructed = Json.decodeFromJsonElement(Name.serializer(), json)
        assertEquals(name, reconstructed)
    }
}