package hep.dataforge.output

import kotlinx.coroutines.Dispatchers

actual val Dispatchers.Output get() = Dispatchers.IO