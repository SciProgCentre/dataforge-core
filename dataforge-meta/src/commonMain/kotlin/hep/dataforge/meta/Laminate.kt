package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.NameToken

/**
 * A meta laminate consisting of multiple immutable meta layers. For mutable front layer, use [Scheme].
 * If [layers] list contains a [Laminate] it is flat-mapped.
 */
class Laminate(layers: List<Meta>) : MetaBase() {

    val layers: List<Meta> = layers.flatMap {
        if (it is Laminate) {
            it.layers
        } else {
            listOf(it)
        }
    }

    constructor(vararg layers: Meta?) : this(layers.filterNotNull())

    override val items: Map<NameToken, MetaItem<Meta>> by lazy {
        layers.map { it.items.keys }.flatten().associateWith { key ->
            layers.asSequence().map { it.items[key] }.filterNotNull().let(replaceRule)
        }
    }

    /**
     * Generate sealed meta using [mergeRule]
     */
    fun merge(): SealedMeta {
        val items = layers.map { it.items.keys }.flatten().associateWith { key ->
            layers.asSequence().map { it.items[key] }.filterNotNull().merge()
        }
        return SealedMeta(items)
    }

    companion object {

        /**
         * The default rule which always uses the first found item in sequence alongside with its children.
         *
         * TODO add picture
         */
        val replaceRule: (Sequence<MetaItem<*>>) -> MetaItem<SealedMeta> = { it.first().seal() }

        private fun Sequence<MetaItem<*>>.merge(): MetaItem<SealedMeta> {
            return when {
                all { it is MetaItem.ValueItem } -> //If all items are values, take first
                    first().seal()
                all { it is MetaItem.NodeItem } -> {
                    //list nodes in item
                    val nodes = map { (it as MetaItem.NodeItem).node }
                    //represent as key->value entries
                    val entries = nodes.flatMap { it.items.entries.asSequence() }
                    //group by keys
                    val groups = entries.groupBy { it.key }
                    // recursively apply the rule
                    val items = groups.mapValues { entry ->
                        entry.value.asSequence().map { it.value }.merge()
                    }
                    MetaItem.NodeItem(SealedMeta(items))

                }
                else -> map {
                    when (it) {
                        is MetaItem.ValueItem -> MetaItem.NodeItem(Meta { Meta.VALUE_KEY put it.value })
                        is MetaItem.NodeItem -> it
                    }
                }.merge()
            }
        }


        /**
         * The values a replaced but meta children are joined
         * TODO add picture
         */
        val mergeRule: (Sequence<MetaItem<*>>) -> MetaItem<SealedMeta> = { it.merge() }
    }
}

/**
 * Performance optimized version of get method
 */
fun Laminate.getFirst(name: Name): MetaItem<*>? {
    layers.forEach { layer ->
        layer[name]?.let { return it }
    }
    return null
}

/**
 * Create a new [Laminate] adding given layer to the top
 */
fun Laminate.withTop(meta: Meta): Laminate = Laminate(listOf(meta) + layers)

/**
 * Create a new [Laminate] adding given layer to the bottom
 */
fun Laminate.withBottom(meta: Meta): Laminate = Laminate(layers + meta)

//TODO add custom rules for Laminate merge
