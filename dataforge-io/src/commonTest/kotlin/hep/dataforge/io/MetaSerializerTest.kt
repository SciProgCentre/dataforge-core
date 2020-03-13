package hep.dataforge.io

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaItem
import hep.dataforge.meta.MetaSerializer
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class MetaSerializerTest {
    @Test
    fun testMetaSerialization() {
        val meta = Meta {
            "a" put 22
            "node" put {
                "b" put "DDD"
                "c" put 11.1
                "array" put doubleArrayOf(1.0, 2.0, 3.0)
            }
        }

        val string = Json.indented.stringify(MetaSerializer, meta)
        val restored = Json.plain.parse(MetaSerializer, string)
        assertEquals(restored, meta)
    }

    @Test
    fun testCborSerialization() {
        val meta = Meta {
            "a" put 22
            "node" put {
                "b" put "DDD"
                "c" put 11.1
                "array" put doubleArrayOf(1.0, 2.0, 3.0)
            }
        }

        val bytes = Cbor.dump(MetaSerializer, meta)
        println(bytes.contentToString())
        val restored = Cbor.load(MetaSerializer, bytes)
        assertEquals(restored, meta)
    }

    @Test
    fun testNameSerialization() {
        val name = "a.b.c".toName()
        val string = Json.indented.stringify(Name.serializer(), name)
        val restored = Json.plain.parse(Name.serializer(), string)
        assertEquals(restored, name)
    }

    @Test
    fun testMetaItemDescriptor() {
        val descriptor = MetaItem.serializer(MetaSerializer).descriptor.getElementDescriptor(0)
    }
}