package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.startsWith

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
 * A transformation which keeps all elements, matching [selector] unchanged.
 */
data class KeepTransformationRule(val selector: (Name) -> Boolean) : TransformationRule {
    override fun matches(name: Name, item: MetaItem<*>?): Boolean {
        return selector(name)
    }

    override fun selectItems(meta: Meta): Sequence<Name> =
        meta.sequence().map { it.first }.filter(selector)

    override fun <M : MutableMeta<M>> transformItem(name: Name, item: MetaItem<*>?, target: M) {
        if (selector(name)) target[name] = item
    }
}

/**
 * A transformation which transforms element with specific name
 */
data class SingleItemTransformationRule(
    val from: Name,
    val transform: MutableMeta<*>.(Name, MetaItem<*>?) -> Unit
) : TransformationRule {
    override fun matches(name: Name, item: MetaItem<*>?): Boolean {
        return name == from
    }

    override fun selectItems(meta: Meta): Sequence<Name> = sequenceOf(from)

    override fun <M : MutableMeta<M>> transformItem(name: Name, item: MetaItem<*>?, target: M) {
        if (name == this.from) {
            target.transform(name, item)
        }
    }
}

data class RegexpItemTransformationRule(
    val from: Regex,
    val transform: MutableMeta<*>.(MatchResult, MetaItem<*>?) -> Unit
) : TransformationRule {
    override fun matches(name: Name, item: MetaItem<*>?): Boolean {
        return from.matches(name.toString())
    }

    override fun <M : MutableMeta<M>> transformItem(name: Name, item: MetaItem<*>?, target: M) {
        val match = from.matchEntire(name.toString())
        if (match != null) {
            target.transform(match, item)
        }
    }

}

inline class MetaTransformation(val transformations: Collection<TransformationRule>) {

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

    /**
     * Listens for changes in the source node and translates them into second node if transformation set contains a corresponding rule.
     */
    fun <M : MutableMeta<M>> bind(source: MutableMeta<*>, target: M) {
        source.onChange(target) { name, oldItem, newItem ->
            transformations.forEach { t ->
                if (t.matches(name, newItem)) {
                    t.transformItem(name, newItem, target)
                }
            }
        }
    }

    companion object {

    }
}

class MetaTransformationBuilder {
    val transformations = HashSet<TransformationRule>()

    fun keep(selector: (Name) -> Boolean) {
        transformations.add(KeepTransformationRule(selector))
    }

    fun keep(name: Name) {
        keep{it == name}
    }

    fun keepNode(name: Name){
        keep{it.startsWith(name)}
    }
}