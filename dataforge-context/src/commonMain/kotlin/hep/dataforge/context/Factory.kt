package hep.dataforge.context

import hep.dataforge.meta.Meta

public interface Factory<out T : Any> {
    public operator fun invoke(meta: Meta = Meta.EMPTY, context: Context = Global): T
}