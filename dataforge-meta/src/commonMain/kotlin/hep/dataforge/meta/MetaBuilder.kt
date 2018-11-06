package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.toName
import hep.dataforge.values.Value

/**
 * DSL builder for meta. Is not intended to store mutable state
 */
class MetaBuilder : MutableMetaNode<MetaBuilder>() {
    override fun wrap(name: Name, meta: Meta): MetaBuilder = meta.builder()
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
            builder[entry.key.toName()] = when (item) {
                is MetaItem.ValueItem -> MetaItem.ValueItem<MetaBuilder>(item.value)
                is MetaItem.NodeItem -> MetaItem.NodeItem(item.node.builder())
            }
        }
    }
}

fun buildMeta(builder: MetaBuilder.() -> Unit): MetaBuilder = MetaBuilder().apply(builder)