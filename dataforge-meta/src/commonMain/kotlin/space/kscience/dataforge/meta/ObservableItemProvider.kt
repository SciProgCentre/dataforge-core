package space.kscience.dataforge.meta

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import space.kscience.dataforge.names.*
import kotlin.js.JsName
import kotlin.jvm.Synchronized
import kotlin.reflect.KProperty1


internal data class ItemListener(
    val owner: Any? = null,
    val action: (name: Name, oldItem: MetaItem?, newItem: MetaItem?) -> Unit,
)

/**
 * An item provider that could be observed and mutated
 */
public interface ObservableItemProvider : ItemProvider, MutableItemProvider {
    /**
     * Add change listener to this meta. Owner is declared to be able to remove listeners later. Listener without owner could not be removed
     */
    public fun onChange(owner: Any?, action: (name: Name, oldItem: MetaItem?, newItem: MetaItem?) -> Unit)

    /**
     * Remove all listeners belonging to given owner
     */
    public fun removeListener(owner: Any?)
}

private open class ObservableItemProviderWrapper(
    open val itemProvider: MutableItemProvider,
    open val parent: Pair<ObservableItemProviderWrapper, Name>? = null
) : ObservableItemProvider {

    override fun getItem(name: Name): MetaItem? = itemProvider.getItem(name)

    private val listeners = HashSet<ItemListener>()

    @Synchronized
    private fun itemChanged(name: Name, oldItem: MetaItem?, newItem: MetaItem?) {
        listeners.forEach { it.action(name, oldItem, newItem) }
    }

    override fun setItem(name: Name, item: MetaItem?) {
        val oldItem = getItem(name)
        itemProvider.setItem(name, item)
        itemChanged(name, oldItem, item)

        //Recursively send update to parent listeners
        parent?.let { (parentNode, token) ->
            parentNode.itemChanged(token + name, oldItem, item)
        }
    }

    @Synchronized
    override fun onChange(owner: Any?, action: (Name, MetaItem?, MetaItem?) -> Unit) {
        listeners.add(ItemListener(owner, action))
    }

    @Synchronized
    override fun removeListener(owner: Any?) {
        listeners.removeAll { it.owner === owner }
    }
}

public fun MutableItemProvider.asObservable(): ObservableItemProvider =
    (this as? ObservableItemProvider) ?: ObservableItemProviderWrapper(this)

/**
 * A Meta instance that could be both mutated and observed.
 */
@Serializable(ObservableMetaSerializer::class)
public interface ObservableMeta : ObservableItemProvider, MutableMeta<ObservableMeta>

/**
 * A wrapper class that creates observable meta node from regular meta node
 */
private class ObservableMetaWrapper<M : MutableMeta<M>>(
    override val itemProvider: M,
    override val parent: Pair<ObservableMetaWrapper<M>, Name>? = null
) : ObservableItemProviderWrapper(itemProvider, parent), ObservableMeta {
    override fun equals(other: Any?): Boolean = (itemProvider == other)

    override fun hashCode(): Int = itemProvider.hashCode()

    override fun toString(): String = itemProvider.toString()

    private fun wrapItem(name: Name, item: TypedMetaItem<M>): TypedMetaItem<ObservableMeta> {
        return when (item) {
            is MetaItemValue -> item
            is MetaItemNode<M> -> ObservableMetaWrapper(item.node, this to name).asMetaItem()
        }
    }

    override fun getItem(name: Name): TypedMetaItem<ObservableMeta>? = itemProvider[name]?.let {
        wrapItem(name, it)
    }

    override val items: Map<NameToken, TypedMetaItem<ObservableMeta>>
        get() = itemProvider.items.mapValues { (token, childItem: TypedMetaItem<M>) ->
            wrapItem(token.asName(), childItem)
        }
}

/**
 * If this meta is observable return itself. Otherwise return an observable wrapper. The changes of initial meta are
 * reflected on wrapper but are **NOT** observed.
 */
public fun <M : MutableMeta<M>> M.asObservable(): ObservableMeta =
    (this as? ObservableMeta) ?: ObservableMetaWrapper(this)

@JsName("buildObservableMeta")
public fun ObservableMeta(): ObservableMeta = MetaBuilder().asObservable()

/**
 * Use the value of the property in a [callBack].
 * The callback is called once immediately after subscription to pass the initial value.
 *
 * Optional [owner] property is used for
 */
public fun <O : ObservableItemProvider, T> O.useProperty(
    property: KProperty1<O, T>,
    owner: Any? = null,
    callBack: O.(T) -> Unit,
) {
    //Pass initial value.
    callBack(property.get(this))
    onChange(owner) { name, oldItem, newItem ->
        if (name.startsWith(property.name.asName()) && oldItem != newItem) {
            callBack(property.get(this))
        }
    }
}

public object ObservableMetaSerializer : KSerializer<ObservableMeta> {
    public fun empty(): ObservableMeta = ObservableMeta()
    override val descriptor: SerialDescriptor get() = MetaSerializer.descriptor

    override fun deserialize(decoder: Decoder): ObservableMeta {
        return MetaSerializer.deserialize(decoder).toMutableMeta().asObservable()
    }

    override fun serialize(encoder: Encoder, value: ObservableMeta) {
        MetaSerializer.serialize(encoder, value)
    }
}

public operator fun ObservableMeta.get(token: NameToken): TypedMetaItem<ObservableMeta>? = items[token]

/**
 * Create a copy of this config, optionally applying the given [block].
 * The listeners of the original Config are not retained.
 */
public inline fun ObservableMeta.copy(block: ObservableMeta.() -> Unit = {}): ObservableMeta =
    asObservable().apply(block)