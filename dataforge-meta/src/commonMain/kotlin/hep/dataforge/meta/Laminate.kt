package hep.dataforge.meta

import hep.dataforge.names.NameToken

/**
 * A meta laminate consisting of multiple immutable meta layers. For mutable front layer, use [StyledConfig].
 *
 *
 */
class Laminate(val layers: List<Meta>) : Meta {

    override val items: Map<NameToken, MetaItem<out Meta>>
        get() = layers.map { it.items.keys }.flatten().associateWith { key ->
            layers.asSequence().map { it.items[key] }.filterNotNull().let(replaceRule)
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
                    val nodes = map { it.node }
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
                        is MetaItem.ValueItem -> MetaItem.NodeItem(buildMeta { Meta.VALUE_KEY to it.value })
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

//TODO add custom rules for Laminate merge
