package space.kscience.dataforge.workspace

import space.kscience.dataforge.actions.AbstractAction
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import kotlin.reflect.KType

internal class CachingAction<T>(
    type: KType, private val caching: (NamedData<T>) -> NamedData<T>
) : AbstractAction<T, T>(type) {

    override fun DataBuilderScope<T>.generate(
        source: DataTree<T>,
        meta: Meta
    ): Map<Name, Data<T>> = buildMap {
        source.forEach {
            val cached = caching(it)
            put(cached.name, cached)
        }
    }

    override suspend fun DataSink<T>.update(source: DataTree<T>, actionMeta: Meta, updateName: Name) {
        val updatedData = source.read(updateName)
        write(updateName, updatedData?.named(updateName)?.let(caching))
    }
}