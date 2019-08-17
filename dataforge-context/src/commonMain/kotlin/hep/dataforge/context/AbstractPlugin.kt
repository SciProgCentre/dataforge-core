package hep.dataforge.context

import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import hep.dataforge.names.toName

abstract class AbstractPlugin(override val meta: Meta = EmptyMeta) : Plugin {
    private var _context: Context? = null

    override val context: Context
        get() = _context ?: error("Plugin $tag is not attached")

    override fun attach(context: Context) {
        this._context = context
    }

    override fun detach() {
        this._context = null
    }

    override fun provideTop(target: String): Map<Name, Any>  = emptyMap()

    companion object{
        fun <T: Named> Collection<T>.toMap(): Map<Name, T> = associate { it.name.toName() to it }
    }
}