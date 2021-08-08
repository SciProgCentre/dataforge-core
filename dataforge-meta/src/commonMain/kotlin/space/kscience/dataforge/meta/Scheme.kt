package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.*
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.values.Value

/**
 * A base for delegate-based or descriptor-based scheme. [Scheme] has an empty constructor to simplify usage from [Specification].
 * Default item provider and [MetaDescriptor] are optional
 */
public open class Scheme : Described, MetaRepr, MutableMetaProvider, Configurable {

    private var _meta = MutableMeta()

    final override val meta: ObservableMutableMeta get() = _meta

    internal var metaDescriptor: MetaDescriptor? = null

    final override val descriptor: MetaDescriptor? get() = metaDescriptor

    internal fun wrap(
        items: MutableMeta,
        preserveDefault: Boolean = false
    ) {
        _meta = (if (preserveDefault) items.withDefault(meta.seal()) else items).asObservable()
    }

    /**
     * Check if property with given [name] could be assigned to [meta]
     */
    public open fun validate(name: Name, meta: Meta?): Boolean {
        val descriptor = descriptor?.get(name)
        return descriptor?.validate(meta) ?: true
    }

    override fun getMeta(name: Name): MutableMeta? = meta.getMeta(name)

    override fun setMeta(name: Name, node: Meta?) {
        if (validate(name, meta)) {
            meta.setMeta(name, node)
        } else {
            error("Validation failed for node $node at $name")
        }
    }

    override fun setValue(name: Name, value: Value?) {
        //TODO add validation
        meta.setValue(name,value)
    }

    override fun toMeta(): Laminate = Laminate(meta, descriptor?.defaultNode)
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

    override fun read(source: Meta): T = builder().also {
        it.wrap(MutableMeta().withDefault(source))
    }

    override fun write(target: MutableMeta): T = empty().also {
        it.wrap(target)
    }

    //TODO Generate descriptor from Scheme class
    override val descriptor: MetaDescriptor? get() = null

    override fun empty(): T = builder().also {
        it.metaDescriptor = descriptor
    }

    @Suppress("OVERRIDE_BY_INLINE")
    final override inline operator fun invoke(action: T.() -> Unit): T = empty().apply(action)

}