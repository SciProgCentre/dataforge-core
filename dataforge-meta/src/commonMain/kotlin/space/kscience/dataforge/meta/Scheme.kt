package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.*
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.*
import space.kscience.dataforge.values.Value
import kotlin.jvm.Synchronized

/**
 * A base for delegate-based or descriptor-based scheme. [Scheme] has an empty constructor to simplify usage from [Specification].
 * Default item provider and [MetaDescriptor] are optional
 */
public open class Scheme : Described, MetaRepr, MutableMetaProvider, Configurable {

    private var targetMeta: MutableMeta = MutableMeta()

    private var defaultMeta: Meta? = null

    final override val meta: ObservableMutableMeta = SchemeMeta(Name.EMPTY)

    final override var descriptor: MetaDescriptor? = null
        internal set

    internal fun wrap(
        newMeta: MutableMeta,
        preserveDefault: Boolean = false
    ) {
        if(preserveDefault){
            defaultMeta = targetMeta.seal()
        }
        targetMeta = newMeta
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
        val valueDescriptor = descriptor?.get(name)
        if (valueDescriptor?.validate(value) != false) {
            meta.setValue(name, value)
        } else error("Value $value is not validated by $valueDescriptor")
    }

    override fun toMeta(): Laminate = Laminate(meta, descriptor?.defaultNode)

    private val listeners = HashSet<MetaListener>()

    private inner class SchemeMeta(val pathName: Name) : ObservableMutableMeta {
        override var value: Value?
            get() = targetMeta[pathName]?.value ?: defaultMeta?.get(pathName)?.value
            set(value) {
                val oldValue = targetMeta[pathName]?.value
                targetMeta[pathName] = value
                if (oldValue != value) {
                    invalidate(pathName)
                }
            }

        override val items: Map<NameToken, ObservableMutableMeta>
            get() {
                val targetKeys = targetMeta[pathName]?.items?.keys ?: emptySet()
                val defaultKeys = defaultMeta?.get(pathName)?.items?.keys ?: emptySet()
                return (targetKeys + defaultKeys).associateWith { SchemeMeta(pathName + it) }
            }

        override fun invalidate(name: Name) {
            listeners.forEach { it.callback(this@Scheme.meta, pathName + name) }
        }

        @Synchronized
        override fun onChange(owner: Any?, callback: Meta.(name: Name) -> Unit) {
            listeners.add(MetaListener(owner) { changedeName ->
                if (changedeName.startsWith(pathName)) {
                    this@Scheme.meta.callback(changedeName.removeHeadOrNull(pathName)!!)
                }
            })
        }

        @Synchronized
        override fun removeListener(owner: Any?) {
            listeners.removeAll { it.owner === owner }
        }

        override fun toString(): String = Meta.toString(this)
        override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)
        override fun hashCode(): Int = Meta.hashCode(this)

        override fun setMeta(name: Name, node: Meta?) {
            targetMeta.setMeta(name, node)
            invalidate(name)
        }

        override fun getOrCreate(name: Name): ObservableMutableMeta = SchemeMeta(pathName + name)

        @DFExperimental
        override fun attach(name: Name, node: ObservableMutableMeta) {
            TODO("Not yet implemented")
        }


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

    override fun read(source: Meta): T = builder().also {
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