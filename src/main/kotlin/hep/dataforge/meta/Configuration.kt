package hep.dataforge.meta

//TODO add validator to configuration

class Configuration: MutableMetaNode<Configuration>() {
    /**
     * Attach configuration node instead of creating one
     */
    override fun wrap(meta: Meta): Configuration {
        return meta as? Configuration ?: Configuration().also { builder ->
            items.mapValues { entry ->
                val item = entry.value
                builder[entry.key] = when (item) {
                    is MetaItem.ValueItem -> MetaItem.ValueItem(item.value)
                    is MetaItem.SingleNodeItem -> MetaItem.SingleNodeItem(wrap(item.node))
                    is MetaItem.MultiNodeItem -> MetaItem.MultiNodeItem(item.nodes.map { wrap(it) })
                }
            }
        }
    }

    override fun empty(): Configuration = Configuration()
}