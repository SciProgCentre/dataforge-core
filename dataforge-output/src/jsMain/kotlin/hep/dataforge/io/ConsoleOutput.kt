package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.context.Global
import hep.dataforge.meta.Meta
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * System console output.
 * The [ConsoleOutput] is used when no other [OutputManager] is provided.
 */
actual val ConsoleOutput: Output<Any> = object : Output<Any> {
    override fun render(obj: Any, meta: Meta) {
        println(obj)
    }

    override val context: Context get() = Global

}

actual val Dispatchers.Output: CoroutineDispatcher get() = Dispatchers.Default