package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta

public fun interface Factory<out T> {
    public fun build(context: Context, meta: Meta): T
}

public operator fun <T> Factory<T>.invoke(
    meta: Meta = Meta.EMPTY,
    context: Context = Global,
): T = build(context, meta)