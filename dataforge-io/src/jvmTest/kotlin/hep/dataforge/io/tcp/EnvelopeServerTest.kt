package hep.dataforge.io.tcp

import hep.dataforge.context.Global
import hep.dataforge.io.Envelope
import hep.dataforge.io.Responder
import hep.dataforge.io.TaggedEnvelopeFormat
import hep.dataforge.io.writeBytes
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

@ExperimentalStdlibApi
object EchoResponder : Responder {
    override suspend fun respond(request: Envelope): Envelope {
        val string = TaggedEnvelopeFormat().run { writeBytes(request).decodeToString() }
        println("ECHO:")
        println(string)
        return request
    }
}

@ExperimentalTime
@ExperimentalStdlibApi
class EnvelopeServerTest {
    companion object {
        @JvmStatic
        val echoEnvelopeServer = EnvelopeServer(Global, 7778, EchoResponder, GlobalScope)

        @BeforeAll
        @JvmStatic
        fun start() {
            echoEnvelopeServer.start()
        }

        @AfterAll
        @JvmStatic
        fun close() {
            echoEnvelopeServer.stop()
        }
    }

    @Test
    fun doEchoTest() {
        val request = Envelope.invoke {
            type = "test.echo"
            meta {
                "test.value" put 22
            }
            data {
                writeDouble(22.7)
            }
        }
        val client = EnvelopeClient(Global, host = "localhost", port = 7778)
        runBlocking {
            val response = client.respond(request)


            assertEquals(request.meta, response.meta)
//            assertEquals(request.data?.toBytes()?.decodeToString(), response.data?.toBytes()?.decodeToString())
            client.close()
        }
    }
}