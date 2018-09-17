package hep.dataforge.meta

import hep.dataforge.names.Name

/**
 * A configuration decorator with applied style
 */
class StyledConfig(val config: Configuration, val style: Meta = EmptyMeta) : MutableMeta<StyledConfig> {

    override fun onChange(owner: Any?, action: (Name, MetaItem<*>?, MetaItem<*>?) -> Unit) {
        config.onChange(owner, action)
    }

    override fun removeListener(owner: Any) {
        config.removeListener(owner)
    }

    override fun set(name: Name, item: MetaItem<StyledConfig>?) {
        when (item) {
            null -> config.remove(name)
            is MetaItem.ValueItem -> config[name] = item.value
            is MetaItem.SingleNodeItem -> config[name] = item.node
            is MetaItem.MultiNodeItem -> config[name] = item.nodes
        }
    }

    override val items: Map<String, MetaItem<StyledConfig>>
        get() = (config.items.keys + style.items.keys).associate { key ->
            val value = config.items[key]
            val styleValue = style[key]
            val item: MetaItem<StyledConfig> = when (value) {
                null -> when (styleValue) {
                    null -> error("Should be unreachable")
                    is MetaItem.ValueItem -> MetaItem.ValueItem(styleValue.value)
                    is MetaItem.SingleNodeItem -> MetaItem.SingleNodeItem(StyledConfig(config.empty(), styleValue.node))
                    is MetaItem.MultiNodeItem -> MetaItem.MultiNodeItem(styleValue.nodes.map { StyledConfig(config.empty(), it) })
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

interface Styleable : Configurable {
    val styledConfig: StyledConfig

    override val config
        get() = styledConfig.config

    val style
        get() = styledConfig.style
}