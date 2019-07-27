package hep.dataforge.meta

import hep.dataforge.names.Name
import hep.dataforge.names.NameToken
import hep.dataforge.names.asName
import hep.dataforge.names.plus

//TODO add validator to configuration

/**
 * Mutable meta representing object state
 */
class Config : AbstractMutableMeta<Config>() {

    private val listeners = HashSet<MetaListener>()

    private fun itemChanged(name: Name, oldItem: MetaItem<*>?, newItem: MetaItem<*>?) {
        listeners.forEach { it.action(name, oldItem, newItem) }
    }

    /**
     * Add change listener to this meta. Owner is declared to be able to remove listeners later. Listener without owner could not be removed
     */
    fun onChange(owner: Any?, action: (Name, MetaItem<*>?, MetaItem<*>?) -> Unit) {
        listeners.add(MetaListener(owner, action))
    }

    /**
     * Remove all listeners belonging to given owner
     */
    fun removeListener(owner: Any?) {
        listeners.removeAll { it.owner === owner }
    }

    override fun replaceItem(key: NameToken, oldItem: MetaItem<Config>?, newItem: MetaItem<Config>?) {
        if (newItem == null) {
            _items.remove(key)
            if(oldItem!= null && oldItem is MetaItem.NodeItem<Config>) {
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
    override fun wrapNode(meta: Meta): Config = meta.toConfig()

    override fun empty(): Config = Config()

    companion object {
        fun empty(): Config = Config()
    }
}

operator fun Config.get(token: NameToken): MetaItem<Config>? = items[token]

fun Meta.toConfig(): Config = this as? Config ?: Config().also { builder ->
    this.items.mapValues { entry ->
        val item = entry.value
        builder[entry.key.asName()] = when (item) {
            is MetaItem.ValueItem -> item.value
            is MetaItem.NodeItem -> MetaItem.NodeItem(item.node.toConfig())
        }
    }
}

interface Configurable {
    val config: Config
}

fun <T : Configurable> T.configure(meta: Meta): T = this.apply { config.update(meta) }

fun <T : Configurable> T.configure(action: MetaBuilder.() -> Unit): T = configure(buildMeta(action))

open class SimpleConfigurable(override val config: Config) : Configurable