package hep.dataforge.meta

import hep.dataforge.names.NameToken

/**
 * The meta implementation which is guaranteed to be immutable.
 *
 * If the argument is possibly mutable node, it is copied on creation
 */
public class SealedMeta internal constructor(
    override val items: Map<NameToken, TypedMetaItem<SealedMeta>>,
) : AbstractTypedMeta<SealedMeta>()

/**
 * Generate sealed node from [this]. If it is already sealed return it as is
 */
public fun Meta.seal(): SealedMeta = this as? SealedMeta ?: SealedMeta(items.mapValues { entry -> entry.value.seal() })

@Suppress("UNCHECKED_CAST")
public fun MetaItem.seal(): TypedMetaItem<SealedMeta> = when (this) {
    is ValueItem -> this
    is NodeItem -> NodeItem(node.seal())
}