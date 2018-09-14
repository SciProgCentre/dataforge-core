package hep.dataforge.meta

/**
 * DSL builder for meta
 */
class MetaBuilder : MutableMetaNode<MetaBuilder>() {
    override fun wrap(meta: Meta): MetaBuilder = meta.builder()
    override fun empty(): MetaBuilder = MetaBuilder()

    infix fun String.to(value: Any) {
        this@MetaBuilder[this] = Value.of(value)
    }

    infix fun String.to(metaBuilder: MetaBuilder.() -> Unit) {
        this@MetaBuilder[this] = MetaBuilder().apply(metaBuilder)
    }
}

/**
 * For safety, builder always copies the initial meta even if it is builder itself
 */
fun Meta.builder(): MetaBuilder {
    return MetaBuilder().also { builder ->
        items.mapValues { entry ->
            val item = entry.value
            builder[entry.key] = when (item) {
                is MetaItem.ValueItem -> MetaItem.ValueItem(item.value)
                is MetaItem.SingleNodeItem -> MetaItem.SingleNodeItem(item.node.builder())
                is MetaItem.MultiNodeItem -> MetaItem.MultiNodeItem(item.nodes.map { it.builder() })
            }
        }
    }
}

fun buildMeta(builder: MetaBuilder.() -> Unit): Meta = MetaBuilder().apply(builder)