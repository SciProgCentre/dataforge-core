package space.kscience.dataforge.misc

@Suppress("UNCHECKED_CAST")
public actual inline fun <T> Any?.unsafeCast(): T = this as T