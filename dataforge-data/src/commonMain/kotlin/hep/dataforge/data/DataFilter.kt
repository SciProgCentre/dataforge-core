package hep.dataforge.data

import hep.dataforge.meta.*
import hep.dataforge.names.toName


class DataFilter(override val config: Config) : Specific {
    var from by string()
    var to by string()
    var pattern by string("*.")
//    val prefix by string()
//    val suffix by string()

    companion object : Specification<DataFilter> {
        override fun wrap(config: Config): DataFilter = DataFilter(config)
    }
}

/**
 * Apply meta-based filter to given data node
 */
fun <T : Any> DataNode<T>.filter(filter: DataFilter): DataNode<T> {
    val sourceNode = filter.from?.let { getNode(it.toName()) } ?: this@filter
    val regex = filter.pattern.toRegex()
    val targetNode = DataTreeBuilder(type).apply {
        sourceNode.data().forEach { (name, data) ->
            if (name.toString().matches(regex)) {
                this[name] = data
            }
        }
    }
    return filter.to?.let {
        DataTreeBuilder(type).apply { this[it.toName()] = targetNode }.build()
    } ?: targetNode.build()
}

/**
 * Filter data using [DataFilter] specification
 */
fun <T : Any> DataNode<T>.filter(filter: Meta): DataNode<T> = filter(DataFilter.wrap(filter))

/**
 * Filter data using [DataFilter] builder
 */
fun <T : Any> DataNode<T>.filter(filterBuilder: DataFilter.() -> Unit): DataNode<T> =
    filter(DataFilter.build(filterBuilder))