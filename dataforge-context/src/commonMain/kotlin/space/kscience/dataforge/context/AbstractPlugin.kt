package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.Named
import space.kscience.dataforge.names.Name
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

public abstract class AbstractPlugin(override val meta: Meta = Meta.EMPTY) : Plugin {
    private var _context: Context? = null
    private val dependencies = HashMap<PluginFactory<*>, Meta>()

    override val isAttached: Boolean get() = _context != null

    override val context: Context
        get() = _context ?: error("Plugin $tag is not attached")

    override fun attach(context: Context) {
        this._context = context
    }

    override fun detach() {
        this._context = null
    }

    final override fun dependsOn(): Map<PluginFactory<*>, Meta> = dependencies

    /**
     * Register plugin dependency and return a delegate which provides lazily initialized reference to dependent plugin
     */
    protected fun <P : Plugin> require(
        factory: PluginFactory<P>,
        meta: Meta = Meta.EMPTY,
    ): ReadOnlyProperty<AbstractPlugin, P> {
        dependencies[factory] = meta
        return PluginDependencyDelegate(factory.type)
    }
}

public fun <T : Named> Collection<T>.toMap(): Map<Name, T> = associate { it.name to it }

private class PluginDependencyDelegate<P : Plugin>(val type: KClass<out P>) : ReadOnlyProperty<AbstractPlugin, P> {
    override fun getValue(thisRef: AbstractPlugin, property: KProperty<*>): P {
        if (!thisRef.isAttached) error("Plugin dependency must not be called eagerly during initialization.")
        return thisRef.context.plugins[type] ?: error("Plugin with type $type not found")
    }
}