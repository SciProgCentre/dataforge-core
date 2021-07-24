package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.*
import space.kscience.dataforge.names.Name

/**
 * A base for delegate-based or descriptor-based scheme. [Scheme] has an empty constructor to simplify usage from [Specification].
 * Default item provider and [NodeDescriptor] are optional
 */
public open class Scheme internal constructor(
    source: MutableMeta = MutableMeta()
) : Described, ObservableMutableMeta, Meta by source {

    private var source = source.asObservable()

    final override var descriptor: MetaDescriptor? = null
        internal set


    internal fun wrap(
        items: MutableMeta,
        preserveDefault: Boolean = false
    ) {
        this.source = if (preserveDefault) items.withDefault(this.source) else items
    }
//
//    /**
//     * Get a property with default
//     */
//    override fun getItem(name: Name): MetaItem? = source[name] ?: descriptor?.get(name)?.defaultValue

    /**
     * Check if property with given [name] could be assigned to [item]
     */
    public open fun validate(name: Name, item: Meta?): Boolean {
        val descriptor = descriptor?.get(name)
        return descriptor?.validateItem(item) ?: true
    }

    /**
     * Set a configurable property
     */
    override fun set(name: Name, meta: Meta) {
        val oldItem = source[name]
        if (oldItem != meta) {
            if (validate(name, meta)) {
                source[name] = meta
            } else {
                error("Validation failed for property $name with value $meta")
            }
        }
    }

    override fun toMeta(): Laminate = Laminate(source, descriptor?.defaultMeta)
}

/**
 * Relocate scheme target onto given [MutableTypedMeta]. Old provider does not get updates anymore.
 * Current state of the scheme used as a default.
 */
public fun <T : Scheme> T.retarget(provider: MutableMeta): T = apply {
    wrap(provider, true)
}

/**
 * A shortcut to edit a [Scheme] object in-place
 */
public inline operator fun <T : Scheme> T.invoke(block: T.() -> Unit): T = apply(block)

/**
 * A specification for simplified generation of wrappers
 */
public open class SchemeSpec<out T : Scheme>(
    private val builder: () -> T,
) : Specification<T>, Described {

    override fun read(items: Meta): T = empty().also {
        it.wrap(MutableMeta().withDefault(items))
    }

    override fun write(target: MutableMeta): T = empty().also {
        it.wrap(target)
    }

    //TODO Generate descriptor from Scheme class
    override val descriptor: MetaDescriptor? get() = null

    override fun empty(): T = builder().also {
        it.descriptor = descriptor
    }

    @Suppress("OVERRIDE_BY_INLINE")
    final override inline operator fun invoke(action: T.() -> Unit): T = empty().apply(action)

}