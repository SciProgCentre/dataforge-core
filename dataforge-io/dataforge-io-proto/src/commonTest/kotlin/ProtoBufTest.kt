package pace.kscience.dataforge.io.proto

import kotlinx.io.readString
import kotlinx.io.writeString
import space.kscience.dataforge.io.Envelope
import space.kscience.dataforge.io.toByteArray
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.asValue
import space.kscience.dataforge.meta.get
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ProtoBufTest {

    @Test
    fun testProtoBufMetaFormat(){
        val meta = Meta {
            "a" put 22
            "node" put {
                "b" put "DDD"
                "c" put 11.1
                "d" put {
                    "d1" put {
                        "d11" put "aaa"
                        "d12" put "bbb"
                    }
                    "d2" put 2
                }
                "array" put doubleArrayOf(1.0, 2.0, 3.0)
                "array2d" put listOf(
                    doubleArrayOf(1.0, 2.0, 3.0).asValue(),
                    doubleArrayOf(1.0, 2.0, 3.0).asValue()
                ).asValue()
            }
        }
        val buffer = kotlinx.io.Buffer()
        ProtoMetaFormat.writeTo(buffer,meta)
        val result = ProtoMetaFormat.readFrom(buffer)

//        println(result["a"]?.value)

        meta.items.keys.forEach {
            assertEquals(meta[it],result[it],"${meta[it]} != ${result[it]}")
        }

        assertEquals(meta, result)
    }

    @Test
    fun testProtoBufEnvelopeFormat(){
        val envelope = Envelope{
            meta {
                "a" put 22
                "node" put {
                    "b" put "DDD"
                    "c" put 11.1
                    "d" put {
                        "d1" put {
                            "d11" put "aaa"
                            "d12" put "bbb"
                        }
                        "d2" put 2
                    }
                    "array" put doubleArrayOf(1.0, 2.0, 3.0)
                    "array2d" put listOf(
                        doubleArrayOf(1.0, 2.0, 3.0).asValue(),
                        doubleArrayOf(1.0, 2.0, 3.0).asValue()
                    ).asValue()
                }
            }
            data {
                writeString("Hello world!")
            }
        }

        val buffer = kotlinx.io.Buffer()
        ProtoEnvelopeFormat.writeTo(buffer,envelope)
//        println(buffer.copy().readString())
        val result = ProtoEnvelopeFormat.readFrom(buffer)

        assertEquals(envelope.meta, result.meta)
        assertContentEquals(envelope.data?.toByteArray(), result.data?.toByteArray())
    }
}