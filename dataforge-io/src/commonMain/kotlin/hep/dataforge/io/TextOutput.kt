package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.io.TextRenderer.Companion.TEXT_RENDERER_TYPE
import hep.dataforge.meta.Meta
import hep.dataforge.provider.Type
import hep.dataforge.provider.provideAll
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class TextOutput(override val context: Context, private val output: kotlinx.io.core.Output) : Output<Any> {
    private val cache = HashMap<KClass<*>, TextRenderer>()

    /**
     * Find the first [TextRenderer] matching the given object type.
     */
    override fun render(obj: Any, meta: Meta) {
        val renderer: TextRenderer = if (obj is CharSequence) {
            DefaultTextRenderer
        } else {
            val value = cache[obj::class]
            if (value == null) {
                val answer = context.provideAll(TEXT_RENDERER_TYPE).filterIsInstance<TextRenderer>()
                    .filter { it.type.isInstance(obj) }.firstOrNull()
                if (answer != null) {
                    cache[obj::class] = answer
                    answer
                } else {
                    DefaultTextRenderer
                }
            } else {
                value
            }
        }
        context.launch(OutputDispatcher) {
            renderer.run { output.render(obj) }
        }
    }
}

@Type(TEXT_RENDERER_TYPE)
interface TextRenderer {
    /**
     * The priority of this renderer compared to other renderers
     */
    val priority: Int
    /**
     * The type of the content served by this renderer
     */
    val type: KClass<*>

    suspend fun kotlinx.io.core.Output.render(obj: Any)

    companion object {
        const val TEXT_RENDERER_TYPE = "dataforge.textRenderer"
    }
}

object DefaultTextRenderer : TextRenderer {
    override val priority: Int = Int.MAX_VALUE
    override val type: KClass<*> = Any::class

    override suspend fun kotlinx.io.core.Output.render(obj: Any) {
        append(obj.toString())
        append('\n')
    }
}