package hep.dataforge.io

import hep.dataforge.context.Global
import kotlinx.coroutines.Dispatchers
import kotlinx.io.streams.asOutput

/**
 * System console output.
 * The [ConsoleOutput] is used when no other [OutputManager] is provided.
 */
actual val ConsoleOutput: Output<Any> = TextOutput(Global, System.out.asOutput())

actual val Dispatchers.Output get() = Dispatchers.IO