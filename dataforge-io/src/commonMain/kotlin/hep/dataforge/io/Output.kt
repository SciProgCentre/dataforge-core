package hep.dataforge.io

import hep.dataforge.context.ContextAware
import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta

/**
 * A generic way to render any object in the output.
 */
interface Output<in T: Any> : ContextAware {
    fun render(obj: T, meta: Meta = EmptyMeta)
}