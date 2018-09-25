package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.toName

/**
 * A configuration decorator with applied style
 */
class StyledConfig(val config: Config, style: Meta = EmptyMeta) : Config() {

    var style: Meta = style
        set(value) {
            field.items.forEach {
                itemChanged(it.key.toName(), it.value, null)
            }
            field = value
            value.items.forEach {
                itemChanged(it.key.toName(), null, it.value)
            }
        }

    init {
        config.onChange { name, oldItem, newItem -> this.itemChanged(name, oldItem, newItem) }
    }

    override fun set(name: Name, item: MetaItem<Config>?) {
        when (item) {
            null -> config.remove(name)
            is MetaItem.ValueItem -> config[name] = item.value
            is MetaItem.SingleNodeItem -> config[name] = item.node
            is MetaItem.MultiNodeItem -> config[name] = item.nodes
        }
    }

    override val items: Map<String, MetaItem<Config>>
        get() = (config.items.keys + style.items.keys).associate { key ->
            val value = config.items[key]
            val styleValue = style[key]
            val item: MetaItem<Config> = when (value) {
                null -> when (styleValue) {
                    null -> error("Should be unreachable")
                    is MetaItem.ValueItem -> MetaItem.ValueItem(styleValue.value)
                    is MetaItem.SingleNodeItem -> MetaItem.SingleNodeItem<Config>(StyledConfig(config.empty(), styleValue.node))
                    is MetaItem.MultiNodeItem -> MetaItem.MultiNodeItem<Config>(styleValue.nodes.map { StyledConfig(config.empty(), it) })
                }
                is MetaItem.ValueItem -> MetaItem.ValueItem(value.value)
                is MetaItem.SingleNodeItem -> MetaItem.SingleNodeItem(
                        StyledConfig(value.node, styleValue?.node ?: EmptyMeta)
                )
                is MetaItem.MultiNodeItem -> MetaItem.MultiNodeItem(value.nodes.map {
                    StyledConfig(it, styleValue?.node ?: EmptyMeta)
                })
            }
            key to item
        }
}

fun Config.withStyle(style: Meta = EmptyMeta) = if (this is StyledConfig) {
    StyledConfig(this.config, style)
} else {
    StyledConfig(this, style)
}

interface Styleable : Configurable {
    override val config: StyledConfig

    var style
        get() = config.style
        set(value) {
            config.style = value
        }
}