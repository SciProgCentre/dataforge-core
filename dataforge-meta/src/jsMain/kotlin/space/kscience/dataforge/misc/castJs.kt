package space.kscience.dataforge.misc
import kotlin.js.unsafeCast as unsafeCastJs

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
public actual inline fun <T> Any?.unsafeCast(): T = this.unsafeCastJs<T>()