package pace.kscience.dataforge.io.proto

import kotlinx.io.writeString
import space.kscience.dataforge.io.Envelope
import space.kscience.dataforge.meta.asValue
import kotlin.concurrent.thread
import kotlin.time.measureTime

public fun main() {
    val envelope = Envelope {
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

    val format = ProtoEnvelopeFormat

    measureTime {
        val threads = List(100) {
            thread {
                repeat(100000) {
                    val buffer = kotlinx.io.Buffer()
                    format.writeTo(buffer, envelope)
//                    println(buffer.size)
                    val r = format.readFrom(buffer)
                }
            }
        }

        threads.forEach { it.join() }
    }.also { println(it) }
}