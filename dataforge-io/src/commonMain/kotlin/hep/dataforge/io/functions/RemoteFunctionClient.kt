package hep.dataforge.io.functions

import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.io.*
import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.int

class RemoteFunctionClient(override val context: Context, val responder: Responder) : FunctionServer, ContextAware {

    private fun <T : Any> IOPlugin.encodeOne(
        meta: Meta,
        value: T
    ): Envelope = Envelope.build {
        meta(meta)
        type = REQUEST_TYPE
        data {
            val inputFormat: IOFormat<T> = getInputFormat<T>(meta)
            inputFormat.run {
                writeThis(value)
            }
        }
    }

    private fun <T : Any> IOPlugin.encodeMany(
        meta: Meta,
        values: List<T>
    ): Envelope = Envelope.build {
        meta(meta)
        type = REQUEST_TYPE
        meta {
            SIZE_KEY to values.size
        }
        data {
            val inputFormat: IOFormat<T> = getInputFormat<T>(meta)
            inputFormat.run {
                values.forEach {
                    writeThis(it)
                }
            }
        }
    }

    private fun <R : Any> IOPlugin.decode(envelope: Envelope): List<R> {
        require(envelope.type == RESPONSE_TYPE) { "Unexpected message type: ${envelope.type}" }
        val size = envelope.meta[SIZE_KEY].int ?: 1

        return if (size == 0) {
            emptyList()
        } else {
            val outputFormat: IOFormat<R> = getOutputFormat<R>(envelope.meta)
            envelope.data?.read {
                List<R>(size) {
                    outputFormat.run {
                        readThis()
                    }
                }
            } ?: error("Message does not contain data")
        }
    }

    private val plugin by lazy {
        context.plugins.load(IOPlugin)
    }

    override suspend fun <T : Any, R : Any> call(
        meta: Meta,
        arg: T
    ): R = plugin.run {
        val request = encodeOne(meta, arg)
        val response = responder.respond(request)
        return decode<R>(response).first()
    }

    override suspend fun <T : Any, R : Any> callMany(
        meta: Meta,
        arg: List<T>
    ): List<R> = plugin.run {
        val request = encodeMany(meta, arg)
        val response = responder.respond(request)
        return decode<R>(response)
    }

    companion object {
        const val REQUEST_TYPE = "function.request"
        const val RESPONSE_TYPE = "function.response"

        const val SIZE_KEY = "size"
    }
}