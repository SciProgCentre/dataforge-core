package hep.dataforge.context

import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class AbstractPlugin(override val meta: Meta = EmptyMeta) : Plugin {
    private var _context: Context? = null
    private val dependencies = ArrayList<PluginFactory<*>>()

    override val context: Context
        get() = _context ?: error("Plugin $tag is not attached")

    override fun attach(context: Context) {
        this._context = context
    }

    override fun detach() {
        this._context = null
    }

    final override fun dependsOn(): List<PluginFactory<*>> = dependencies

    /**
     * Register plugin dependency and return a delegate which provides lazily initialized reference to dependent plugin
     */
    protected fun <P : Plugin> require(factory: PluginFactory<P>): ReadOnlyProperty<AbstractPlugin, P> {
        dependencies.add(factory)
        return PluginDependencyDelegate(factory.type)
    }

    override fun provideTop(target: String): Map<Name, Any> = emptyMap()
}

fun <T : Named> Collection<T>.toMap(): Map<Name, T> = associate { it.name to it }

private class PluginDependencyDelegate<P : Plugin>(val type: KClass<out P>) : ReadOnlyProperty<AbstractPlugin, P> {
    override fun getValue(thisRef: AbstractPlugin, property: KProperty<*>): P {
        return thisRef.context.plugins[type] ?: error("Plugin with type $type not found")
    }
}