package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.Described
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.get
import space.kscience.dataforge.meta.descriptors.validate
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.misc.ThreadSafe
import space.kscience.dataforge.names.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A base for delegate-based or descriptor-based scheme. [Scheme] has an empty constructor to simplify usage from [MetaSpec].
 * Default item provider and [MetaDescriptor] are optional
 */
public open class Scheme : Described, MetaRepr, MutableMetaProvider, Configurable {

    /**
     * Meta to be mutated by this scheme
     */
    private var target: MutableMeta? = null
        get() {
            // automatic initialization of target if it is missing
            if (field == null) {
                field = MutableMeta()
            }
            return field
        }

    /**
     * Default values provided by this scheme
     */
    private var prototype: Meta? = null

    final override val meta: ObservableMutableMeta = SchemeMeta(Name.EMPTY)

    final override var descriptor: MetaDescriptor? = null
        private set

    /**
     * This method must be called before the scheme could be used
     */
    internal fun initialize(
        target: MutableMeta,
        prototype: Meta,
        descriptor: MetaDescriptor?,
    ) {
        this.target = target
        this.prototype = prototype
        this.descriptor = descriptor
    }

    /**
     * Check if property with given [name] could be assigned to [meta]
     */
    public open fun validate(name: Name, meta: Meta?): Boolean {
        val descriptor = descriptor?.get(name)
        return descriptor?.validate(meta) ?: true
    }

    override fun get(name: Name): MutableMeta? = meta[name]

    override fun set(name: Name, node: Meta?) {
        if (validate(name, meta)) {
            meta[name] = node
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

    private val listeners: MutableList<MetaListener> = mutableListOf()

    override fun toString(): String = meta.toString()

    private inner class SchemeMeta(val pathName: Name) : ObservableMutableMeta {
        override var value: Value?
            get() = target[pathName]?.value
                ?: prototype?.get(pathName)?.value
                ?: descriptor?.get(pathName)?.defaultValue
            set(value) {
                val oldValue = target[pathName]?.value
                target!![pathName] = value
                if (oldValue != value) {
                    invalidate(Name.EMPTY)
                }
            }

        override val items: Map<NameToken, ObservableMutableMeta>
            get() {
                val targetKeys = target[pathName]?.items?.keys ?: emptySet()
                val defaultKeys = prototype?.get(pathName)?.items?.keys ?: emptySet()
                return (targetKeys + defaultKeys).associateWith { SchemeMeta(pathName + it) }
            }

        override fun invalidate(name: Name) {
            listeners.forEach { it.callback(this@Scheme.meta, pathName + name) }
        }

        @ThreadSafe
        override fun onChange(owner: Any?, callback: Meta.(name: Name) -> Unit) {
            listeners.add(MetaListener(owner) { changedName ->
                if (changedName.startsWith(pathName)) {
                    this@Scheme.meta.callback(changedName.removeFirstOrNull(pathName)!!)
                }
            })
        }

        @ThreadSafe
        override fun removeListener(owner: Any?) {
            listeners.removeAll { it.owner === owner }
        }

        override fun toString(): String = Meta.toString(this)
        override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)
        override fun hashCode(): Int = Meta.hashCode(this)

        override fun set(name: Name, node: Meta?) {
            target!![name] = node
            invalidate(name)
        }

        override fun getOrCreate(name: Name): ObservableMutableMeta = SchemeMeta(pathName + name)

        @DFExperimental
        override fun attach(name: Name, node: ObservableMutableMeta) {
            set(name, node)
            node.onChange(this) { changeName ->
                set(name + changeName, this[changeName])
            }
        }

    }
}

/**
 * Relocate scheme target onto given [MutableMeta]. Old provider does not get updates anymore.
 * The Current state of the scheme that os used as a default.
 */
@DFExperimental
public fun <T : Scheme> T.retarget(provider: MutableMeta): T = apply {
    initialize(provider, meta.seal(), descriptor)
}

/**
 * A shortcut to edit a [Scheme] object in-place
 */
public inline operator fun <T : Scheme> T.invoke(block: T.() -> Unit): T = apply(block)

/**
 * Create a copy of given [Scheme]
 */
public inline fun <T : Scheme> T.copy(spec: SchemeSpec<T>, block: T.() -> Unit = {}): T =
    spec.read(meta.copy()).apply(block)

/**
 * A specification for simplified generation of wrappers
 */
public open class SchemeSpec<out T : Scheme>(
    private val builder: () -> T,
) : MetaSpec<T> {

    override val descriptor: MetaDescriptor? get() = null

    override fun readOrNull(source: Meta): T = builder().also {
        it.initialize(MutableMeta(), source, descriptor)
    }

    public fun write(target: MutableMeta): T = empty().also {
        it.initialize(target, Meta.EMPTY, descriptor)
    }

    /**
     * Generate an empty object
     */
    public fun empty(): T = builder().also {
        it.initialize(MutableMeta(), Meta.EMPTY, descriptor)
    }

    /**
     * A convenience method to use specifications in builders
     */
    public inline operator fun invoke(action: T.() -> Unit): T = empty().apply(action)

}



/**
 * Update a [MutableMeta] using given specification
 */
public fun <T : Scheme> MutableMeta.updateWith(
    spec: SchemeSpec<T>,
    action: T.() -> Unit,
): T = spec.write(this).apply(action)


/**
 * Update configuration using given specification
 */
public fun <T : Scheme> Configurable.updateWith(
    spec: SchemeSpec<T>,
    action: T.() -> Unit,
): T = spec.write(meta).apply(action)


/**
 * A delegate that uses a [MetaSpec] to wrap a child of this provider
 */
public fun <T : Scheme> MutableMeta.scheme(
    spec: SchemeSpec<T>,
    key: Name? = null,
): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val name = key ?: property.name.asName()
        return spec.write(getOrCreate(name))
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val name = key ?: property.name.asName()
        set(name, value.toMeta())
    }
}

public fun <T : Scheme> Scheme.scheme(
    spec: SchemeSpec<T>,
    key: Name? = null,
): ReadWriteProperty<Any?, T> = meta.scheme(spec, key)

/**
 * A delegate that uses a [MetaSpec] to wrap a child of this provider.
 * Returns null if meta with given name does not exist.
 */
public fun <T : Scheme> MutableMeta.schemeOrNull(
    spec: SchemeSpec<T>,
    key: Name? = null,
): ReadWriteProperty<Any?, T?> = object : ReadWriteProperty<Any?, T?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        val name = key ?: property.name.asName()
        return if (get(name) == null) null else spec.write(getOrCreate(name))
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        val name = key ?: property.name.asName()
        if (value == null) remove(name)
        else set(name, value.toMeta())
    }
}

public fun <T : Scheme> Scheme.schemeOrNull(
    spec: SchemeSpec<T>,
    key: Name? = null,
): ReadWriteProperty<Any?, T?> = meta.schemeOrNull(spec, key)

/**
 * A delegate that uses a [MetaSpec] to wrap a list of child providers.
 * If children are mutable, the changes in list elements are reflected on them.
 * The list is a snapshot of children state, so change in structure is not reflected on its composition.
 */
@DFExperimental
public fun <T : Scheme> MutableMeta.listOfScheme(
    spec: SchemeSpec<T>,
    key: Name? = null,
): ReadWriteProperty<Any?, List<T>> = object : ReadWriteProperty<Any?, List<T>> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): List<T> {
        val name = key ?: property.name.asName()
        return getIndexed(name).values.map { spec.write(it as MutableMeta) }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<T>) {
        val name = key ?: property.name.asName()
        setIndexed(name, value.map { it.toMeta() })
    }
}


@DFExperimental
public fun <T : Scheme> Scheme.listOfScheme(
    spec: SchemeSpec<T>,
    key: Name? = null,
): ReadWriteProperty<Any?, List<T>> = meta.listOfScheme(spec, key)
