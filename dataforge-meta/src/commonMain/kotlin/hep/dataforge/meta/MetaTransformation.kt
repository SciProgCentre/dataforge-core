package hep.dataforge.meta

import hep.dataforge.names.Name

/**
 * A transformation for meta item or a group of items
 */
interface TransformationRule {

    /**
     * Check if this transformation
     */
    fun matches(name: Name, item: MetaItem<*>?): Boolean

    /**
     * Select all items to be transformed. Item could be a value as well as node
     *
     * @return a sequence of item paths to be transformed
     */
    fun selectItems(meta: Meta): Sequence<Name> =
        meta.sequence().filter { matches(it.first, it.second) }.map { it.first }

    /**
     * Apply transformation for a single item (Node or Value) and return resulting tree with absolute path
     */
    fun <M : MutableMeta<M>> transformItem(name: Name, item: MetaItem<*>?, target: M): Unit
}

/**
 * A transformation which transforms an element with given [name] to itself.
 */
data class SelfTransformationRule(val name: Name) : TransformationRule {
    override fun matches(name: Name, item: MetaItem<*>?): Boolean {
        return name == name
    }

    override fun selectItems(meta: Meta): Sequence<Name> = sequenceOf(name)

    override fun <M : MutableMeta<M>> transformItem(name: Name, item: MetaItem<*>?, target: M) {
        if (name == this.name) target[name] = item
    }
}

/**
 * A transformation which transforms element with specific name
 */
data class SingleItemTransformationRule(
    val from: Name,
    val to: Name,
    val transform: MutableMeta<*>.(MetaItem<*>?) -> Unit
) : TransformationRule {
    override fun matches(name: Name, item: MetaItem<*>?): Boolean {
        return name == from
    }

    override fun selectItems(meta: Meta): Sequence<Name> = sequenceOf(from)

    override fun <M : MutableMeta<M>> transformItem(name: Name, item: MetaItem<*>?, target: M) {
        if (name == this.from) {
            target.transform(item)
        }
    }
}

class MetaTransformation {
    private val transformations = HashSet<TransformationRule>()


    /**
     * Produce new meta using only those items that match transformation rules
     */
    fun transform(source: Meta): Meta = buildMeta {
        transformations.forEach { rule ->
            rule.selectItems(source).forEach { name ->
                rule.transformItem(name, source[name], this)
            }
        }
    }

    /**
     * Transform a meta, replacing all elements found in rules with transformed entries
     */
    fun apply(source: Meta): Meta = buildMeta(source) {
        transformations.forEach { rule ->
            rule.selectItems(source).forEach { name ->
                remove(name)
                rule.transformItem(name, source[name], this)
            }
        }
    }

    companion object{

    }
}