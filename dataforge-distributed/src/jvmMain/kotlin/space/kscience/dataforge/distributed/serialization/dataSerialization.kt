package space.kscience.dataforge.distributed.serialization

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.Goal
import space.kscience.dataforge.data.await
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaSerializer
import kotlin.reflect.KType

/**
 * [Data] representation that is trivially serializable.
 */
@Serializable
internal data class DataPrototype(
    val meta: String,
    val data: String,
) {
    fun toData(type: KType): Data<Any> =
        SimpleData(
            type = type,
            meta = Json.decodeFromString(MetaSerializer, meta),
            data = Json.decodeFromString(kotlinx.serialization.serializer(type), data)!!
        )

    companion object {
        suspend fun <T : Any> of(data: Data<T>, serializer: KSerializer<in T>): DataPrototype {
            val meta = Json.encodeToString(MetaSerializer, data.meta)
            val string = Json.encodeToString(serializer, data.await())
            return DataPrototype(meta, string)
        }
    }
}

/**
 * Trivial [Data] implementation.
 */
private class SimpleData<T : Any>(
    override val type: KType,
    override val meta: Meta,
    val data: T,
) : Data<T> {
    override val dependencies: Collection<Goal<*>>
        get() = emptyList()

    override val deferred: Deferred<T>
        get() = CompletableDeferred(data)

    override fun async(coroutineScope: CoroutineScope): Deferred<T> = deferred
    override fun reset() = Unit
}
