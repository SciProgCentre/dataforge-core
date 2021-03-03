package space.kscience.dataforge.output

import space.kscience.dataforge.context.ContextAware
import space.kscience.dataforge.meta.Meta

/**
 * A generic way to render any object in the output.
 *
 * An object could be rendered either in append or overlay mode. The mode is decided by the [Renderer]
 * based on its configuration and provided meta
 *
 */
public fun interface Renderer<in T : Any> {
    /**
     * Render specific object with configuration.
     *
     * By convention actual render is called in asynchronous mode, so this method should never
     * block execution
     */
    public fun render(obj: T, meta: Meta)
}
