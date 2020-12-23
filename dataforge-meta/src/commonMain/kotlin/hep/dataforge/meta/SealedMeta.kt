package hep.dataforge.meta

import hep.dataforge.names.NameToken

/**
 * The meta implementation which is guaranteed to be immutable.
 *
 * If the argument is possibly mutable node, it is copied on creation
 */
public class SealedMeta internal constructor(
    override val items: Map<NameToken, MetaItem<SealedMeta>>,
) : AbstractTypedMeta<SealedMeta>()

/**
 * Generate sealed node from [this]. If it is already sealed return it as is
 */
public fun Meta.seal(): SealedMeta = this as? SealedMeta ?: SealedMeta(items.mapValues { entry -> entry.value.seal() })

@Suppress("UNCHECKED_CAST")
public fun MetaItem<*>.seal(): MetaItem<SealedMeta> = when (this) {
    is MetaItem.ValueItem -> this
    is MetaItem.NodeItem -> MetaItem.NodeItem(node.seal())
}