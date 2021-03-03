package space.kscience.dataforge.meta

import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken

/**
 * A meta laminate consisting of multiple immutable meta layers. For mutable front layer, use [Scheme].
 * If [layers] list contains a [Laminate] it is flat-mapped.
 */
public class Laminate(layers: List<Meta>) : MetaBase() {

    public val layers: List<Meta> = layers.flatMap {
        if (it is Laminate) {
            it.layers
        } else {
            listOf(it)
        }
    }

    override val items: Map<NameToken, TypedMetaItem<Meta>> by lazy {
        layers.map { it.items.keys }.flatten().associateWith { key ->
            layers.asSequence().map { it.items[key] }.filterNotNull().let(replaceRule)
        }
    }

    /**
     * Generate sealed meta using [mergeRule]
     */
    public fun merge(): SealedMeta {
        val items = layers.map { it.items.keys }.flatten().associateWith { key ->
            layers.asSequence().map { it.items[key] }.filterNotNull().merge()
        }
        return SealedMeta(items)
    }

    public companion object {

        /**
         * The default rule which always uses the first found item in sequence alongside with its children.
         *
         * TODO add picture
         */
        public val replaceRule: (Sequence<MetaItem>) -> TypedMetaItem<SealedMeta> = { it.first().seal() }

        private fun Sequence<MetaItem>.merge(): TypedMetaItem<SealedMeta> {
            return when {
                all { it is MetaItemValue } -> //If all items are values, take first
                    first().seal()
                all { it is MetaItemNode } -> {
                    //list nodes in item
                    val nodes = map { (it as MetaItemNode).node }
                    //represent as key->value entries
                    val entries = nodes.flatMap { it.items.entries.asSequence() }
                    //group by keys
                    val groups = entries.groupBy { it.key }
                    // recursively apply the rule
                    val items = groups.mapValues { entry ->
                        entry.value.asSequence().map { it.value }.merge()
                    }
                    MetaItemNode(SealedMeta(items))

                }
                else -> map {
                    when (it) {
                        is MetaItemValue -> MetaItemNode(Meta { Meta.VALUE_KEY put it.value })
                        is MetaItemNode -> it
                    }
                }.merge()
            }
        }


        /**
         * The values a replaced but meta children are joined
         * TODO add picture
         */
        public val mergeRule: (Sequence<MetaItem>) -> TypedMetaItem<SealedMeta> = { it.merge() }
    }
}

@Suppress("FunctionName")
public fun Laminate(vararg layers: Meta?): Laminate = Laminate(layers.filterNotNull())

/**
 * Performance optimized version of get method
 */
public fun Laminate.getFirst(name: Name): MetaItem? {
    layers.forEach { layer ->
        layer[name]?.let { return it }
    }
    return null
}

/**
 * Create a new [Laminate] adding given layer to the top
 */
public fun Laminate.withTop(meta: Meta): Laminate = Laminate(listOf(meta) + layers)

/**
 * Create a new [Laminate] adding given layer to the bottom
 */
public fun Laminate.withBottom(meta: Meta): Laminate = Laminate(layers + meta)

//TODO add custom rules for Laminate merge
