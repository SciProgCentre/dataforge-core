package hep.dataforge.context

import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta

interface Factory<out T : Any> {
    operator fun invoke(meta: Meta = EmptyMeta, context: Context = Global): T
}