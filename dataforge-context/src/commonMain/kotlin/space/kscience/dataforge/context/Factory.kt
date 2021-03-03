package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta

public interface Factory<out T : Any> {
    public operator fun invoke(meta: Meta = Meta.EMPTY, context: Context = Global): T
}