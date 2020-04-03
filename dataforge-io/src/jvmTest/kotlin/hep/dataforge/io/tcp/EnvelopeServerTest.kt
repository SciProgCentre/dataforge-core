package hep.dataforge.io.tcp

import hep.dataforge.context.Global
import hep.dataforge.io.Envelope
import hep.dataforge.io.Responder
import hep.dataforge.io.TaggedEnvelopeFormat
import hep.dataforge.io.toByteArray
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import kotlinx.io.writeDouble
import org.junit.AfterClass
import org.junit.BeforeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

@ExperimentalStdlibApi
object EchoResponder : Responder {
    override suspend fun respond(request: Envelope): Envelope {
        val string = TaggedEnvelopeFormat().run { toByteArray(request).decodeToString() }
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

        @BeforeClass
        @JvmStatic
        fun start() {
            echoEnvelopeServer.start()
        }

        @AfterClass
        @JvmStatic
        fun close() {
            echoEnvelopeServer.stop()
        }
    }

    @Test(timeout = 1000)
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