package space.kscience.dataforge.io

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaSerializer
import space.kscience.dataforge.meta.TypedMetaItem
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.toName
import kotlin.test.Test
import kotlin.test.assertEquals

val JSON_PRETTY: Json = Json { prettyPrint = true; useArrayPolymorphism = true }
val JSON_PLAIN: Json = Json { prettyPrint = false; useArrayPolymorphism = true }

class MetaSerializerTest {
    val meta = Meta {
        "a" put 22
        "node" put {
            "b" put "DDD"
            "c" put 11.1
            "array" put doubleArrayOf(1.0, 2.0, 3.0)
        }
    }

    @Test
    fun testMetaSerialization() {
        val string = JSON_PRETTY.encodeToString(MetaSerializer, meta)
        println(string)
        val restored = JSON_PLAIN.decodeFromString(MetaSerializer, string)
        assertEquals(meta, restored)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testCborSerialization() {
        val bytes = Cbor.encodeToByteArray(MetaSerializer, meta)
        println(bytes.decodeToString())
        val restored = Cbor.decodeFromByteArray(MetaSerializer, bytes)
        assertEquals(meta, restored)
    }

    @Test
    fun testNameSerialization() {
        val name = "a.b.c".toName()
        val string = JSON_PRETTY.encodeToString(Name.serializer(), name)
        val restored = JSON_PLAIN.decodeFromString(Name.serializer(), string)
        assertEquals(name, restored)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testMetaItemDescriptor() {
        val descriptor = TypedMetaItem.serializer(MetaSerializer).descriptor.getElementDescriptor(0)
        println(descriptor)
    }
}