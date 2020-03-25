package hep.dataforge.output

import hep.dataforge.context.ContextAware
import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta

/**
 * A generic way to render any object in the output.
 *
 * An object could be rendered either in append or overlay mode. The mode is decided by the [Renderer]
 * based on its configuration and provided meta
 *
 */
interface Renderer<in T : Any> : ContextAware {
    /**
     * Render specific object with configuration.
     *
     * By convention actual render is called in asynchronous mode, so this method should never
     * block execution
     */
    fun render(obj: T, meta: Meta = EmptyMeta)
}
