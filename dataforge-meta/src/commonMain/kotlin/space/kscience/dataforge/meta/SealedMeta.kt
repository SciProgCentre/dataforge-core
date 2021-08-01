package space.kscience.dataforge.meta

import kotlinx.serialization.Serializable
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.values.Value

/**
 * The meta implementation which is guaranteed to be immutable.
 *
 * If the argument is possibly mutable node, it is copied on creation
 */
@Serializable
public class SealedMeta internal constructor(
    override val value: Value?,
    override val items: Map<NameToken, SealedMeta>
) : TypedMeta<SealedMeta> {
    override fun toString(): String = Meta.toString(this)
    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)
    override fun hashCode(): Int = Meta.hashCode(this)
}

/**
 * Generate sealed node from [this]. If it is already sealed return it as is
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
public inline fun Meta(builder: MutableMeta.() -> Unit): SealedMeta =
    MutableMeta(builder).seal()

