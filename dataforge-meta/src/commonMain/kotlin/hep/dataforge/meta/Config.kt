package hep.dataforge.meta

import hep.dataforge.names.Name

//TODO add validator to configuration

/**
 * Mutable meta representing object state
 */
open class Config : MutableMetaNode<Config>() {

    /**
     * Attach configuration node instead of creating one
     */
    override fun wrap(name: Name, meta: Meta): Config = meta.toConfig()

    override fun empty(): Config = Config()
}

fun Meta.toConfig(): Config = this as? Config ?: Config().also { builder ->
    this.items.mapValues { entry ->
        val item = entry.value
        builder[entry.key] = when (item) {
            is MetaItem.ValueItem -> MetaItem.ValueItem(item.value)
            is MetaItem.SingleNodeItem -> MetaItem.SingleNodeItem(item.node.toConfig())
            is MetaItem.MultiNodeItem -> MetaItem.MultiNodeItem(item.nodes.map { it.toConfig() })
        }
    }
}

interface Configurable {
    val config: Config
}

fun <T : Configurable> T.configure(meta: Meta): T = this.apply { config.update(meta) }

fun <T : Configurable> T.configure(action: Config.() -> Unit): T = this.apply { config.apply(action) }

open class SimpleConfigurable(override val config: Config) : Configurable