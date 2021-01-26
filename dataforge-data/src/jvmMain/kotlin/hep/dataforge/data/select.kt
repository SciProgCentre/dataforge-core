package hep.dataforge.data

import hep.dataforge.actions.NamedData
import hep.dataforge.actions.named
import hep.dataforge.meta.DFExperimental
import hep.dataforge.names.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass


/**
 * Select all data matching given type and filters. Does not modify paths
 */
@OptIn(DFExperimental::class)
public fun <R : Any> DataSet<*>.select(
    type: KClass<out R>,
    namePattern: Name? = null,
): ActiveDataSet<R> = object : ActiveDataSet<R> {
    override val dataType: KClass<out R> = type

    @Suppress("UNCHECKED_CAST")
    override fun flow(): Flow<NamedData<R>> = this@select.flow().filter {
        it.canCast(type) && (namePattern == null || it.name.matches(namePattern))
    }.map {
        it as NamedData<R>
    }

    override suspend fun getData(name: Name): Data<R>? = this@select.getData(name)?.castOrNull(type)

    override val updates: Flow<Name> = this@select.updates.filter {
        val datum = this@select.getData(it)
        datum?.canCast(type) ?: false
    }

}

/**
 * Select a single datum of the appropriate type
 */
public inline fun <reified R : Any> DataSet<*>.select(namePattern: Name? = null): DataSet<R> =
    select(R::class, namePattern)

public suspend fun <R : Any> DataSet<*>.selectOne(type: KClass<out R>, name: Name): NamedData<R>? =
    getData(name)?.castOrNull(type)?.named(name)

public suspend inline fun <reified R : Any> DataSet<*>.selectOne(name: Name): NamedData<R>? = selectOne(R::class, name)

public suspend inline fun <reified R : Any> DataSet<*>.selectOne(name: String): NamedData<R>? =
    selectOne(R::class, name.toName())