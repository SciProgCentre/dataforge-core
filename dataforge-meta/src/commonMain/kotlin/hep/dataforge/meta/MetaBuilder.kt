package hep.dataforge.meta

import hep.dataforge.names.asName
import hep.dataforge.values.Value

/**
 * DSL builder for meta. Is not intended to store mutable state
 */
class MetaBuilder : AbstractMutableMeta<MetaBuilder>() {
    override fun wrapNode(meta: Meta): MetaBuilder = meta.builder()
    override fun empty(): MetaBuilder = MetaBuilder()

    infix fun String.to(value: Any) {
        if (value is Meta) {
            this@MetaBuilder[this] = value
        }
        this@MetaBuilder[this] = Value.of(value)
    }

    infix fun String.to(meta: Meta) {
        this@MetaBuilder[this] = meta
    }

    infix fun String.to(value: Iterable<Meta>) {
        this@MetaBuilder[this] = value.toList()
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
            builder[entry.key.asName()] = when (item) {
                is MetaItem.ValueItem -> item.value
                is MetaItem.NodeItem -> MetaItem.NodeItem(item.node.builder())
            }
        }
    }
}

/**
 * Build a [MetaBuilder] using given transformation
 */
fun buildMeta(builder: MetaBuilder.() -> Unit): MetaBuilder = MetaBuilder().apply(builder)

/**
 * Build meta using given source meta as a base
 */
fun buildMeta(source: Meta, builder: MetaBuilder.() -> Unit): MetaBuilder = source.builder().apply(builder)