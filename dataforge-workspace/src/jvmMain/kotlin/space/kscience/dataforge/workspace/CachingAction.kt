package space.kscience.dataforge.workspace

import space.kscience.dataforge.actions.AbstractAction
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import kotlin.reflect.KType

internal class CachingAction<T>(type: KType, private val caching: (NamedData<T>) -> NamedData<T>) :
    AbstractAction<T, T>(type) {
    override fun DataSink<T>.generate(source: DataTree<T>, meta: Meta) {
        source.forEach {
            put(caching(it))
        }
    }

    override suspend fun DataSink<T>.update(source: DataTree<T>, meta: Meta, updatedData: DataUpdate<T>) {
        put(updatedData.name, updatedData.data?.named(updatedData.name)?.let(caching))
    }
}