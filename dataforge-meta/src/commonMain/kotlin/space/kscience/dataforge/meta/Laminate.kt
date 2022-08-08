package space.kscience.dataforge.meta

import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken

/**
 * A meta laminate consisting of multiple immutable meta layers. For mutable front layer, use [Scheme].
 * If [layers] list contains a [Laminate] it is flat-mapped.
 */
public class Laminate internal constructor(public val layers: List<Meta>) : TypedMeta<Laminate> {

    override val value: Value? = layers.firstNotNullOfOrNull { it.value }

    override val items: Map<NameToken, Laminate> by lazy {
        layers.map { it.items.keys }.flatten().associateWith { key ->
            Laminate(layers.mapNotNull { it.items[key] })
        }
    }

    override fun getMeta(name: Name): Laminate? {
        val childLayers = layers.mapNotNull { it.getMeta(name) }
        return if (childLayers.isEmpty()) null else Laminate(childLayers)
    }

    /**
     * Generate sealed meta by interweaving all layers. If a value is present in at least on layer, it will be present
     * in the result.
     */
    public fun merge(): SealedMeta {
        val items = layers.map { it.items.keys }.flatten().associateWith { key ->
            layers.asSequence().map { it.items[key] }.filterNotNull().merge()
        }
        return SealedMeta(value, items)
    }

    /**
     * Generate sealed meta by stacking layers. If node is present in the upper layer, then the lower layers will be
     * ignored event if they have values that are not present on top layer.
     */
    public fun top(): SealedMeta {
        val items = layers.map { it.items.keys }.flatten().associateWith { key ->
            layers.asSequence().map { it.items[key] }.filterNotNull().first().seal()
        }
        return SealedMeta(value, items)
    }

    override fun toString(): String = Meta.toString(this)
    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)
    override fun hashCode(): Int = Meta.hashCode(this)

    public companion object {

        /**
         * The default rule which always uses the first found item in sequence alongside with its children.
         *
         * TODO add picture
         */
        public val replaceRule: (Sequence<Meta>) -> SealedMeta? = { it.firstOrNull()?.seal() }

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
            return SealedMeta(value, items)
        }


        /**
         * The values a replaced but meta children are joined
         * TODO add picture
         */
        public val mergeRule: (Sequence<Meta>) -> SealedMeta? = { it.merge() }
    }
}

public fun Laminate(layers: Collection<Meta?>): Laminate {
    val flatLayers = layers.flatMap {
        if (it is Laminate) {
            it.layers
        } else {
            listOf(it)
        }
    }.filterNotNull()
    return Laminate(flatLayers)
}

public fun Laminate(vararg layers: Meta?): Laminate = Laminate(listOf(*layers))

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
