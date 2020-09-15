package hep.dataforge.data

import hep.dataforge.meta.*
import hep.dataforge.names.toName


public class DataFilter : Scheme() {
    /**
     * A source node for the filter
     */
    public var from: String? by string()
    /**
     * A target placement for the filtered node
     */
    public var to: String? by string()
    /**
     * A regular expression pattern for the filter
     */
    public var pattern: String by string(".*")
//    val prefix by string()
//    val suffix by string()

    public fun isEmpty(): Boolean = config.isEmpty()

    public companion object : SchemeSpec<DataFilter>(::DataFilter)
}

/**
 * Apply meta-based filter to given data node
 */
public fun <T : Any> DataNode<T>.filter(filter: DataFilter): DataNode<T> {
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
public fun <T : Any> DataNode<T>.filter(filter: Meta): DataNode<T> = filter(DataFilter.wrap(filter))

/**
 * Filter data using [DataFilter] builder
 */
public fun <T : Any> DataNode<T>.filter(filterBuilder: DataFilter.() -> Unit): DataNode<T> =
    filter(DataFilter(filterBuilder))