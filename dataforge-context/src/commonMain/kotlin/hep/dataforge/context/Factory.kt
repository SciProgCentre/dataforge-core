package hep.dataforge.context

import hep.dataforge.meta.Meta

interface Factory<out T : Any> {
    operator fun invoke(meta: Meta = Meta.EMPTY, context: Context = Global): T
}