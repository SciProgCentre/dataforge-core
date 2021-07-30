package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.*
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.values.Value

/**
 * A base for delegate-based or descriptor-based scheme. [Scheme] has an empty constructor to simplify usage from [Specification].
 * Default item provider and [NodeDescriptor] are optional
 */
public open class Scheme(
    source: MutableMeta = MutableMeta()
) : Described, MutableMeta, ObservableMeta, Meta by source {

    private var source = source.asObservable()

    final override var descriptor: MetaDescriptor? = null
        internal set

    override var value: Value?
        get() = source.value
        set(value) {
            source.value = value
        }

    override val items: Map<NameToken, MutableMeta> get() = source.items

    internal fun wrap(
        items: MutableMeta,
        preserveDefault: Boolean = false
    ) {
        this.source = (if (preserveDefault) items.withDefault(this.source) else items).asObservable()
    }

    /**
     * Check if property with given [name] could be assigned to [meta]
     */
    public open fun validate(name: Name, meta: Meta?): Boolean {
        val descriptor = descriptor?.get(name)
        return descriptor?.validate(meta) ?: true
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

    override fun toMeta(): Laminate = Laminate(source, descriptor?.defaultNode)

    override fun remove(name: Name) {
        source.remove(name)
    }

    override fun getOrCreate(name: Name): MutableMeta = source.getOrCreate(name)

    override fun onChange(owner: Any?, callback: Meta.(name: Name) -> Unit) {
        source.onChange(owner ?: this, callback)
    }

    override fun removeListener(owner: Any?) {
        source.removeListener(owner ?: this)
    }
}

/**
 * Relocate scheme target onto given [MutableMeta]. Old provider does not get updates anymore.
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

    override fun read(source: Meta): T = empty().also {
        it.wrap(MutableMeta().withDefault(source))
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