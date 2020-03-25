package hep.dataforge.output

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers


actual val Dispatchers.Output: CoroutineDispatcher get() = Dispatchers.Default