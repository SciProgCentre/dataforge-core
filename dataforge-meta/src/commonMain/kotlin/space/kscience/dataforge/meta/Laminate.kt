package space.kscience.dataforge.meta

import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.values.Value

/**
 * A meta laminate consisting of multiple immutable meta layers. For mutable front layer, use [Scheme].
 * If [layers] list contains a [Laminate] it is flat-mapped.
 */
public class Laminate(layers: List<Meta>) : TypedMeta<SealedMeta> {

    override val value: Value? = layers.firstNotNullOfOrNull { it.value }

    public val layers: List<Meta> = layers.flatMap {
        if (it is Laminate) {
            it.layers
        } else {
            listOf(it)
        }
    }

    override val items: Map<NameToken, SealedMeta> by lazy {
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
        return SealedMeta(value, items)
    }

    public companion object {

        /**
         * The default rule which always uses the first found item in sequence alongside with its children.
         *
         * TODO add picture
         */
        public val replaceRule: (Sequence<Meta>) -> SealedMeta = { it.first().seal() }

        private fun Sequence<Meta>.merge(): SealedMeta {
            val value = firstNotNullOfOrNull { it.value }
            //list nodes in item
            val nodes = toList()
            //represent as key->value entries
            val entries = nodes.flatMap { it.items.entries.asSequence() }
            //group by keys
            val groups = entries.groupBy { it.key }
            // recursively apply the rule
            val items = groups.mapValues { entry ->
                entry.value.asSequence().map { it.value }.merge()
            }
            return SealedMeta(value,items)
        }


        /**
         * The values a replaced but meta children are joined
         * TODO add picture
         */
        public val mergeRule: (Sequence<Meta>) -> TypedMeta<SealedMeta> = { it.merge() }
    }
}

@Suppress("FunctionName")
public fun Laminate(vararg layers: Meta?): Laminate = Laminate(layers.filterNotNull())

/**
 * Performance optimized version of get method
 */
public fun Laminate.getFirst(name: Name): Meta? {
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
