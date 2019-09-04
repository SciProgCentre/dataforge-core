package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.io.functions.FunctionSpec
import hep.dataforge.io.functions.FunctionsPlugin
import hep.dataforge.meta.get
import hep.dataforge.meta.int
import kotlin.reflect.KClass

class RemoteFunctionClient(override val context: Context, val responder: Responder) : FunctionServer, ContextAware {

    private fun <T : Any> encodeOne(name: String, spec: FunctionSpec<T, *>, value: T): Envelope =
        Envelope.build {
            type = REQUEST_TYPE
            meta {
                FUNCTION_NAME_KEY to name
                FUNCTION_SPEC_KEY to spec.toMeta()
            }
            data {
                spec.inputFormat.run {
                    writeThis(value)
                }
            }
        }

    private fun <T : Any> encodeMany(name: String, spec: FunctionSpec<T, *>, values: List<T>): Envelope =
        Envelope.build {
            type = REQUEST_TYPE
            meta {
                FUNCTION_NAME_KEY to name
                FUNCTION_SPEC_KEY to spec.toMeta()
                SIZE_KEY to values.size
            }
            data {
                spec.inputFormat.run {
                    values.forEach {
                        writeThis(it)
                    }
                }
            }
        }

    private fun <R : Any> decode(spec: FunctionSpec<*, R>, envelope: Envelope): List<R> {
        require(envelope.type == RESPONSE_TYPE) { "Unexpected message type: ${envelope.type}" }
        val size = envelope.meta[SIZE_KEY].int ?: 1

        return if (size == 0) {
            emptyList()
        } else {
            envelope.data?.read {
                List<R>(size) {
                    spec.outputFormat.run {
                        readThis()
                    }
                }
            } ?: error("Message does not contain data")
        }
    }

    override suspend fun <T : Any, R : Any> call(
        name: String,
        spec: FunctionSpec<T, R>,
        arg: T
    ): R {
        val request = encodeOne(name, spec, arg)
        val response = responder.respond(request)
        return decode(spec, response).first()
    }

    override suspend fun <T : Any, R : Any> callMany(
        name: String,
        spec: FunctionSpec<T, R>,
        arg: List<T>
    ): List<R> {
        val request = encodeMany(name, spec, arg)
        val response = responder.respond(request)
        return decode(spec, response)
    }

    private val plugin by lazy {
        context.plugins.load(FunctionsPlugin)
    }

    fun <T : Any, R : Any> resolveSpec(
        inputType: KClass<out T>,
        outputType: KClass<out R>
    ): FunctionSpec<T, R> {
        return plugin.resolve(inputType, outputType)
    }

    companion object {
        const val REQUEST_TYPE = "function.request"
        const val RESPONSE_TYPE = "function.response"

        const val FUNCTION_NAME_KEY = "function"
        const val SIZE_KEY = "size"
        const val FUNCTION_SPEC_KEY = "spec"
    }
}