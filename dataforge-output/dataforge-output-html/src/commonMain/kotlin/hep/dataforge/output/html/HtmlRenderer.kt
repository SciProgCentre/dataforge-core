package hep.dataforge.output.html

import hep.dataforge.context.Context
import hep.dataforge.meta.DFExperimental
import hep.dataforge.meta.Meta
import hep.dataforge.output.Output
import hep.dataforge.output.Renderer
import hep.dataforge.output.TextFormat
import hep.dataforge.output.html.HtmlBuilder.Companion.HTML_CONVERTER_TYPE
import hep.dataforge.provider.Type
import hep.dataforge.provider.top
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.p
import kotlin.reflect.KClass


@DFExperimental
public class HtmlRenderer<T : Any>(override val context: Context, private val consumer: TagConsumer<*>) : Renderer<T> {
    private val cache = HashMap<KClass<*>, HtmlBuilder<*>>()

    /**
     * Find the first [TextFormat] matching the given object type.
     */
    override fun render(obj: T, meta: Meta) {

        val builder: HtmlBuilder<*> = if (obj is CharSequence) {
            DefaultHtmlBuilder
        } else {
            val value = cache[obj::class]
            if (value == null) {
                val answer =
                    context.top<HtmlBuilder<*>>(HTML_CONVERTER_TYPE).values.firstOrNull { it.type.isInstance(obj) }
                if (answer != null) {
                    cache[obj::class] = answer
                    answer
                } else {
                    DefaultHtmlBuilder
                }
            } else {
                value
            }
        }
        context.launch(Dispatchers.Output) {
            @Suppress("UNCHECKED_CAST")
            (builder as HtmlBuilder<T>).run { render(obj) }
        }
    }
}

/**
 * A text or binary renderer based on [Renderer]
 */
@Type(HTML_CONVERTER_TYPE)
public interface HtmlBuilder<T : Any> {
    /**
     * The priority of this renderer compared to other renderers
     */
    public val priority: Int

    /**
     * The type of the content served by this renderer
     */
    public val type: KClass<T>

    public suspend fun FlowContent.render(obj: T)

    public companion object {
        public const val HTML_CONVERTER_TYPE: String = "dataforge.htmlBuilder"
    }
}

public object DefaultHtmlBuilder : HtmlBuilder<Any> {
    override val priority: Int = Int.MAX_VALUE
    override val type: KClass<Any> = Any::class

    override suspend fun FlowContent.render(obj: Any) {
        p { +obj.toString() }
    }
}