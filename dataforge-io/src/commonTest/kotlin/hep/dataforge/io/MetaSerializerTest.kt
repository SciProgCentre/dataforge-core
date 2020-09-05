package hep.dataforge.io

import hep.dataforge.meta.*
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlin.test.Test
import kotlin.test.assertEquals

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
        val restored = JSON_PLAIN.decodeFromString(MetaSerializer, string)
        assertEquals(meta, restored)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testCborSerialization() {
        val bytes = Cbor.encodeToByteArray(MetaSerializer, meta)
        println(bytes.contentToString())
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

    @Test
    fun testMetaItemDescriptor() {
        val descriptor = MetaItem.serializer(MetaSerializer).descriptor.getElementDescriptor(0)
    }
}