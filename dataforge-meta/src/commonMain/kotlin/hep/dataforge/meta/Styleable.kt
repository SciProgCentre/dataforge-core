package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.NameToken
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
            is MetaItem.NodeItem -> config[name] = item.node
        }
    }

    override val items: Map<NameToken, MetaItem<Config>>
        get() = (config.items.keys + style.items.keys).associate { key ->
            val value = config.items[key]
            val styleValue = style[key]
            val item: MetaItem<Config> = when (value) {
                null -> when (styleValue) {
                    null -> error("Should be unreachable")
                    is MetaItem.ValueItem -> MetaItem.ValueItem(styleValue.value)
                    is MetaItem.NodeItem -> MetaItem.NodeItem<Config>(StyledConfig(config.empty(), styleValue.node))
                }
                is MetaItem.ValueItem -> MetaItem.ValueItem(value.value)
                is MetaItem.NodeItem -> MetaItem.NodeItem(
                        StyledConfig(value.node, styleValue?.node ?: EmptyMeta)
                )
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