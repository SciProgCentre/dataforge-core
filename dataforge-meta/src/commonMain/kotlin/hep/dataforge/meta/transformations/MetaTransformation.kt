package hep.dataforge.meta.transformations

import hep.dataforge.meta.*
import hep.dataforge.misc.DFExperimental
import hep.dataforge.names.Name

/**
 * A transformation for meta item or a group of items
 */
public interface TransformationRule {

    /**
     * Check if this transformation should be applied to a node with given name and value
     */
    public fun matches(name: Name, item: MetaItem?): Boolean

    /**
     * Select all items to be transformed. Item could be a value as well as node
     *
     * @return a sequence of item paths to be transformed
     */
    public fun selectItems(meta: Meta): Sequence<Name> =
        meta.itemSequence().filter { matches(it.first, it.second) }.map { it.first }

    /**
     * Apply transformation for a single item (Node or Value) to the target
     */
    public fun <M : MutableMeta<M>> transformItem(name: Name, item: MetaItem?, target: M): Unit
}

/**
 * A transformation which keeps all elements, matching [selector] unchanged.
 */
public data class KeepTransformationRule(val selector: (Name) -> Boolean) :
    TransformationRule {
    override fun matches(name: Name, item: MetaItem?): Boolean {
        return selector(name)
    }

    override fun selectItems(meta: Meta): Sequence<Name> =
        meta.itemSequence().map { it.first }.filter(selector)

    override fun <M : MutableMeta<M>> transformItem(name: Name, item: MetaItem?, target: M) {
        if (selector(name)) target.set(name, item)
    }
}

/**
 * A transformation which transforms element with specific name
 */
public data class SingleItemTransformationRule(
    val from: Name,
    val transform: MutableMeta<*>.(Name, MetaItem?) -> Unit,
) : TransformationRule {
    override fun matches(name: Name, item: MetaItem?): Boolean {
        return name == from
    }

    override fun selectItems(meta: Meta): Sequence<Name> = sequenceOf(from)

    override fun <M : MutableMeta<M>> transformItem(name: Name, item: MetaItem?, target: M) {
        if (name == this.from) {
            target.transform(name, item)
        }
    }
}

public data class RegexItemTransformationRule(
    val from: Regex,
    val transform: MutableMeta<*>.(name: Name, MatchResult, MetaItem?) -> Unit,
) : TransformationRule {
    override fun matches(name: Name, item: MetaItem?): Boolean {
        return from.matches(name.toString())
    }

    override fun <M : MutableMeta<M>> transformItem(name: Name, item: MetaItem?, target: M) {
        val match = from.matchEntire(name.toString())
        if (match != null) {
            target.transform(name, match, item)
        }
    }

}

/**
 * A set of [TransformationRule] to either transform static meta or create dynamically updated [MutableMeta]
 */
public inline class MetaTransformation(public val transformations: Collection<TransformationRule>) {

    /**
     * Produce new meta using only those items that match transformation rules
     */
    public fun generate(source: Meta): Meta =
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
    public fun generate(source: Config): ObservableItemProvider = Config().apply {
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
    public fun apply(source: Meta): Meta =
        source.toMutableMeta().apply {
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
    public fun <M : MutableMeta<M>> bind(source: Config, target: M) {
        source.onChange(target) { name, _, newItem ->
            transformations.forEach { t ->
                if (t.matches(name, newItem)) {
                    t.transformItem(name, newItem, target)
                }
            }
        }
    }

    public companion object {
        public fun make(block: MetaTransformationBuilder.() -> Unit): MetaTransformation =
            MetaTransformationBuilder().apply(block).build()
    }
}

/**
 * A builder for a set of transformation rules
 */
public class MetaTransformationBuilder {
    private val transformations = HashSet<TransformationRule>()

    /**
     * Keep all items with name satisfying the criteria
     */
    public fun keep(selector: (Name) -> Boolean) {
        transformations.add(KeepTransformationRule(selector))
    }

    /**
     * Keep specific item (including its descendants)
     */
    public fun keep(name: Name) {
        keep { it == name }
    }

    /**
     * Keep nodes by regex
     */
    public fun keep(regex: String) {
        transformations.add(
            RegexItemTransformationRule(regex.toRegex()) { name, _, metaItem ->
                set(name, metaItem)
            })
    }

    /**
     * Move an item from [from] to [to], optionally applying [operation] it defined
     */
    public fun move(from: Name, to: Name, operation: (MetaItem?) -> Any? = { it }) {
        transformations.add(
            SingleItemTransformationRule(from) { _, item ->
                set(to, operation(item))
            }
        )
    }

    public fun build(): MetaTransformation = MetaTransformation(transformations)
}