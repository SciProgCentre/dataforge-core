package hep.dataforge.data

import hep.dataforge.names.Name
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass


public fun <R : Any> DataSet<*>.filterIsInstance(type: KClass<out R>): DataSet<R> = object : DataSet<R> {
    override val dataType: KClass<out R> = type

    @Suppress("UNCHECKED_CAST")
    override fun flow(): Flow<NamedData<R>> = this@filterIsInstance.flow().filter {
        it.canCast(type)
    }.map {
        it as NamedData<R>
    }

    override suspend fun getData(name: Name): Data<R>? = this@filterIsInstance.getData(name)?.castOrNull(type)

    override val updates: Flow<Name> = this@filterIsInstance.updates.filter {
        val datum = this@filterIsInstance.getData(it)
        datum?.canCast(type) ?: false
    }

}

public inline fun <reified R : Any> DataSet<*>.filterIsInstance(): DataSet<R> = filterIsInstance(R::class)