package space.kscience.dataforge.distributed.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.StaticData
import space.kscience.dataforge.data.await
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
    fun <T : Any> toData(type: KType, serializer: KSerializer<T>): Data<T> =
        StaticData(
            type = type,
            value = Json.decodeFromString(serializer, data),
            meta = Json.decodeFromString(MetaSerializer, meta),
        )

    companion object {
        suspend fun <T : Any> of(data: Data<T>, serializer: KSerializer<in T>): DataPrototype {
            val meta = Json.encodeToString(MetaSerializer, data.meta)
            val string = Json.encodeToString(serializer, data.await())
            return DataPrototype(meta, string)
        }
    }
}
