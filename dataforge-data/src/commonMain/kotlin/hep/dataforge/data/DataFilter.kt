package hep.dataforge.data

import hep.dataforge.meta.*
import hep.dataforge.names.toName


class DataFilter : Scheme() {
    /**
     * A source node for the filter
     */
    var from by string()
    /**
     * A target placement for the filtered node
     */
    var to by string()
    /**
     * A regular expression pattern for the filter
     */
    var pattern by string(".*")
//    val prefix by string()
//    val suffix by string()

    fun isEmpty(): Boolean = config.isEmpty()

    companion object : SchemeSpec<DataFilter>(::DataFilter)
}

/**
 * Apply meta-based filter to given data node
 */
fun <T : Any> DataNode<T>.filter(filter: DataFilter): DataNode<T> {
    val sourceNode = filter.from?.let { get(it.toName()).node } ?: this@filter
    val regex = filter.pattern.toRegex()
    val targetNode = DataTreeBuilder(type).apply {
        sourceNode.dataSequence().forEach { (name, data) ->
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
    filter(DataFilter.invoke(filterBuilder))