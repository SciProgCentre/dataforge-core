package space.kscience.dataforge.misc
import kotlin.js.unsafeCast as unsafeCastJs

@Suppress("NOTHING_TO_INLINE")
public actual inline fun <T> Any?.unsafeCast(): T = unsafeCastJs<T>()