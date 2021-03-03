package space.kscience.dataforge.output

import space.kscience.dataforge.context.Context
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.output.TextFormat.Companion.TEXT_RENDERER_TYPE
import space.kscience.dataforge.provider.Type
import space.kscience.dataforge.provider.top
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.reflect.KType


/**
 * A text or binary renderer based on [Output]
 */
@Type(TEXT_RENDERER_TYPE)
@Deprecated("Bad design")
public interface TextFormat {
    /**
     * The priority of this renderer compared to other renderers
     */
    public val priority: Int
    /**
     * The type of the content served by this renderer
     */
    public val type: KClass<*>

    public suspend fun Appendable.render(obj: Any)

    public companion object {
        public const val TEXT_RENDERER_TYPE: String = "dataforge.textRenderer"
    }
}

@Deprecated("Bad design")
public object DefaultTextFormat : TextFormat {
    override val priority: Int = Int.MAX_VALUE
    override val type: KClass<*> = Any::class

    override suspend fun Appendable.render(obj: Any) {
        append(obj.toString() + "\n")
    }
}

/**
 * A text-based renderer
 */
@Deprecated("Bad design")
public class TextRenderer(override val context: Context, private val output: Appendable) : Renderer<Any> {
    private val cache = HashMap<KClass<*>, TextFormat>()

    /**
     * Find the first [TextFormat] matching the given object type.
     */
    override fun render(obj: Any, meta: Meta) {
        val format: TextFormat = if (obj is CharSequence) {
            DefaultTextFormat
        } else {
            val value = cache[obj::class]
            if (value == null) {
                val answer =
                    context.top<TextFormat>(TEXT_RENDERER_TYPE).values.firstOrNull { it.type.isInstance(obj) }
                if (answer != null) {
                    cache[obj::class] = answer
                    answer
                } else {
                    DefaultTextFormat
                }
            } else {
                value
            }
        }
        context.launch(Dispatchers.Output) {
            format.run { output.render(obj) }
        }
    }
}