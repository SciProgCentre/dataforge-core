package hep.dataforge.context

import hep.dataforge.meta.Config
import hep.dataforge.names.Name

abstract class AbstractPlugin : Plugin {
    private var _context: Context? = null

    override val context: Context
        get() = _context ?: error("Plugin $tag is not attached")

    override val config = Config()

    override fun attach(context: Context) {
        this._context = context
    }

    override fun detach() {
        this._context = null
    }

    //TODO make configuration activation-safe

    override fun provideTop(target: String, name: Name): Any? = null

    override fun listTop(target: String): Sequence<Name> = emptySequence()
}