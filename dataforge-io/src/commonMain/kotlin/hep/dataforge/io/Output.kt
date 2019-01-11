package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * A generic way to render any object in the output.
 *
 * An object could be rendered either in append or overlay mode. The mode is decided by the [Output]
 * based on its configuration and provided meta
 *
 */
interface Output<in T : Any> : ContextAware {
    /**
     * Render specific object with configuration.
     *
     * By convention actual render is called in asynchronous mode, so this method should never
     * block execution
     */
    fun render(obj: T, meta: Meta = EmptyMeta)
}
