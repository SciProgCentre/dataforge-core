package hep.dataforge.context

import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name

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

    override fun provideTop(target: String, name: Name): Any? = null

    override fun listTop(target: String): Sequence<Name> = emptySequence()
}