package space.kscience.dataforge.output

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers


public actual val Dispatchers.Output: CoroutineDispatcher get() = Default