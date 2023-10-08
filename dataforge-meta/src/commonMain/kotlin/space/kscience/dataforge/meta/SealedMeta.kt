package space.kscience.dataforge.meta

import kotlinx.serialization.Serializable
import space.kscience.dataforge.names.*
import kotlin.js.JsName

/**
 * The meta implementation which is guaranteed to be immutable.
 *
 */
@Serializable
public class SealedMeta(
    override val value: Value?,
    override val items: Map<NameToken, SealedMeta>,
) : TypedMeta<SealedMeta> {
    override fun toString(): String = Meta.toString(this)
    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)

    /**
     * Compute hash code once to optimize later access
     */
    private val cachedHashCode by lazy {
        Meta.hashCode(this)
    }

    override fun hashCode(): Int = cachedHashCode
}

/**
 * Generate sealed node from [this]. If it is already sealed, return it as is.
 */
public fun Meta.seal(): SealedMeta = this as? SealedMeta ?: SealedMeta(
    value,
    items.mapValues { entry ->
        entry.value.seal()
    }
)

@Suppress("FunctionName")
public fun Meta(value: Value): SealedMeta = SealedMeta(value, emptyMap())

@Suppress("FunctionName")
public fun Meta(value: Number): SealedMeta = Meta(value.asValue())

@Suppress("FunctionName")
public fun Meta(value: String): SealedMeta = Meta(value.asValue())

@Suppress("FunctionName")
public fun Meta(value: Boolean): SealedMeta = Meta(value.asValue())


/**
 * A lightweight mutable meta used to create [SealedMeta] instances without bothering with
 */
@PublishedApi
internal class MetaBuilder(
    override var value: Value? = null,
    override val items: MutableMap<NameToken, MetaBuilder> = hashMapOf(),
) : MutableMeta {

    override fun getOrCreate(name: Name): MetaBuilder {
        val existing = get(name) as? MetaBuilder
        return if (existing == null) {
            val newItem = MetaBuilder()
            setMeta(name, newItem)
            newItem
        } else {
            existing
        }
    }

    private fun wrap(meta: Meta): MetaBuilder = meta as? MetaBuilder ?: MetaBuilder(
        meta.value,
        meta.items.mapValuesTo(hashMapOf()) { wrap(it.value) }
    )


    override fun setMeta(name: Name, node: Meta?) {
        when (name.length) {
            0 -> error("Can't set a meta with empty name")
            1 -> {
                val token = name.first()
                //remove child and invalidate if argument is null
                if (node == null) {
                    items.remove(token)
                } else {
                    items[token] = wrap(node)
                }
            }

            else -> {
                getOrCreate(name.first().asName()).setMeta(name.cutFirst(), node)
            }
        }
    }

    override fun toString(): String = Meta.toString(this)

    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)

    override fun hashCode(): Int = Meta.hashCode(this)
}

/**
 * Create a read-only meta.
 */
public inline fun Meta(builder: MutableMeta.() -> Unit): Meta =
    MetaBuilder().apply(builder).seal()

/**
 * Create an immutable meta.
 */
public inline fun SealedMeta(builder: MutableMeta.() -> Unit): SealedMeta =
    MetaBuilder().apply(builder).seal()

/**
 * Create an empty meta mutable meta.
 */
@JsName("newMutableMeta")
public fun MutableMeta(): MutableMeta = MetaBuilder()

/**
 * Create a mutable meta with given builder.
 */
public inline fun MutableMeta(builder: MutableMeta.() -> Unit = {}): MutableMeta =
    MutableMeta().apply(builder)
