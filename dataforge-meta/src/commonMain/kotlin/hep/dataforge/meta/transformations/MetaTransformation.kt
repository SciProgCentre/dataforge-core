package hep.dataforge.meta.transformations

import hep.dataforge.meta.*
import hep.dataforge.names.Name

/**
 * A transformation for meta item or a group of items
 */
interface TransformationRule {

    /**
     * Check if this transformation should be applied to a node with given name and value
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
     * Apply transformation for a single item (Node or Value) to the target
     */
    fun <M : MutableMeta<M>> transformItem(name: Name, item: MetaItem<*>?, target: M): Unit
}

/**
 * A transformation which keeps all elements, matching [selector] unchanged.
 */
data class KeepTransformationRule(val selector: (Name) -> Boolean) :
    TransformationRule {
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

data class RegexItemTransformationRule(
    val from: Regex,
    val transform: MutableMeta<*>.(name: Name, MatchResult, MetaItem<*>?) -> Unit
) : TransformationRule {
    override fun matches(name: Name, item: MetaItem<*>?): Boolean {
        return from.matches(name.toString())
    }

    override fun <M : MutableMeta<M>> transformItem(name: Name, item: MetaItem<*>?, target: M) {
        val match = from.matchEntire(name.toString())
        if (match != null) {
            target.transform(name, match, item)
        }
    }

}

/**
 * A set of [TransformationRule] to either transform static meta or create dynamically updated [MutableMeta]
 */
inline class MetaTransformation(val transformations: Collection<TransformationRule>) {

    /**
     * Produce new meta using only those items that match transformation rules
     */
    fun generate(source: Meta): Meta =
        Meta {
            transformations.forEach { rule ->
                rule.selectItems(source).forEach { name ->
                    rule.transformItem(name, source[name], this)
                }
            }
        }

    /**
     * Generate an observable configuration that contains only elements defined by transformation rules and changes with the source
     */
    @DFExperimental
    fun generate(source: Config): ObservableMeta = Config().apply {
        transformations.forEach { rule ->
            rule.selectItems(source).forEach { name ->
                rule.transformItem(name, source[name], this)
            }
        }

        bind(source, this)
    }

    /**
     * Transform a meta, replacing all elements found in rules with transformed entries
     */
    fun apply(source: Meta): Meta =
        source.edit {
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
    fun <M : MutableMeta<M>> bind(source: Config, target: M) {
        source.onChange(target) { name, _, newItem ->
            transformations.forEach { t ->
                if (t.matches(name, newItem)) {
                    t.transformItem(name, newItem, target)
                }
            }
        }
    }

    companion object {
        fun make(block: MetaTransformationBuilder.() -> Unit): MetaTransformation =
            MetaTransformationBuilder().apply(block).build()
    }
}

/**
 * A builder for a set of transformation rules
 */
class MetaTransformationBuilder {
    val transformations = HashSet<TransformationRule>()

    /**
     * Keep all items with name satisfying the criteria
     */
    fun keep(selector: (Name) -> Boolean) {
        transformations.add(KeepTransformationRule(selector))
    }

    /**
     * Keep specific item (including its descendants)
     */
    fun keep(name: Name) {
        keep { it == name }
    }

    /**
     * Keep nodes by regex
     */
    fun keep(regex: String) {
        transformations.add(
            RegexItemTransformationRule(regex.toRegex()) { name, _, metaItem ->
                setItem(name, metaItem)
            })
    }

    /**
     * Move an item from [from] to [to], optionally applying [operation] it defined
     */
    fun move(from: Name, to: Name, operation: (MetaItem<*>?) -> Any? = { it }) {
        transformations.add(
            SingleItemTransformationRule(from) { _, item ->
                set(to, operation(item))
            }
        )
    }

    fun build() = MetaTransformation(transformations)
}