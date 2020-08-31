package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.NameToken
import hep.dataforge.names.asName
import hep.dataforge.names.plus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.collections.set

//TODO add validator to configuration

public data class MetaListener(
    val owner: Any? = null,
    val action: (name: Name, oldItem: MetaItem<*>?, newItem: MetaItem<*>?) -> Unit
)

public interface ObservableMeta : Meta {
    public fun onChange(owner: Any?, action: (name: Name, oldItem: MetaItem<*>?, newItem: MetaItem<*>?) -> Unit)
    public fun removeListener(owner: Any?)
}

/**
 * Mutable meta representing object state
 */
@Serializable
public class Config : AbstractMutableMeta<Config>(), ObservableMeta {

    private val listeners = HashSet<MetaListener>()

    private fun itemChanged(name: Name, oldItem: MetaItem<*>?, newItem: MetaItem<*>?) {
        listeners.forEach { it.action(name, oldItem, newItem) }
    }

    /**
     * Add change listener to this meta. Owner is declared to be able to remove listeners later. Listener without owner could not be removed
     */
    override fun onChange(owner: Any?, action: (Name, MetaItem<*>?, MetaItem<*>?) -> Unit) {
        listeners.add(MetaListener(owner, action))
    }

    /**
     * Remove all listeners belonging to given owner
     */
    override fun removeListener(owner: Any?) {
        listeners.removeAll { it.owner === owner }
    }

    override fun replaceItem(key: NameToken, oldItem: MetaItem<Config>?, newItem: MetaItem<Config>?) {
        if (newItem == null) {
            _items.remove(key)
            if (oldItem != null && oldItem is MetaItem.NodeItem<Config>) {
                oldItem.node.removeListener(this)
            }
        } else {
            _items[key] = newItem
            if (newItem is MetaItem.NodeItem) {
                newItem.node.onChange(this) { name, oldChild, newChild ->
                    itemChanged(key + name, oldChild, newChild)
                }
            }
        }
        itemChanged(key.asName(), oldItem, newItem)
    }

    /**
     * Attach configuration node instead of creating one
     */
    override fun wrapNode(meta: Meta): Config = meta.asConfig()

    override fun empty(): Config = Config()

    @OptIn(ExperimentalSerializationApi::class)
    @Serializer(Config::class)
    public companion object ConfigSerializer : KSerializer<Config> {

        public fun empty(): Config = Config()
        override val descriptor: SerialDescriptor get() = MetaSerializer.descriptor

        override fun deserialize(decoder: Decoder): Config {
            return MetaSerializer.deserialize(decoder).asConfig()
        }

        override fun serialize(encoder: Encoder, value: Config) {
            MetaSerializer.serialize(encoder, value)
        }
    }
}

public operator fun Config.get(token: NameToken): MetaItem<Config>? = items[token]

public fun Meta.asConfig(): Config = this as? Config ?: Config().also { builder ->
    this.items.mapValues { entry ->
        val item = entry.value
        builder[entry.key.asName()] = when (item) {
            is MetaItem.ValueItem -> item.value
            is MetaItem.NodeItem -> MetaItem.NodeItem(item.node.asConfig())
        }
    }
}