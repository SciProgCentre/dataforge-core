package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.plus

//TODO add validator to configuration

/**
 * Mutable meta representing object state
 */
class Configuration : MutableMetaNode<Configuration>() {

    /**
     * Attach configuration node instead of creating one
     */
    override fun wrap(name: Name, meta: Meta): Configuration {
        return meta as? Configuration ?: Configuration().also { builder ->
            meta.items.mapValues { entry ->
                val item = entry.value
                builder[entry.key] = when (item) {
                    is MetaItem.ValueItem -> MetaItem.ValueItem(item.value)
                    is MetaItem.SingleNodeItem -> MetaItem.SingleNodeItem(wrap(name + entry.key, item.node))
                    is MetaItem.MultiNodeItem -> MetaItem.MultiNodeItem(item.nodes.map { wrap(name + entry.key, it) })
                }
            }
        }
    }


    override fun empty(): Configuration = Configuration()
}

interface Configurable {
    val config: Configuration
}

open class SimpleConfigurable(override val config: Configuration) : Configurable