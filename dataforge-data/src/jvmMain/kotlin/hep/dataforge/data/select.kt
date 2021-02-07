package hep.dataforge.data

import hep.dataforge.misc.DFExperimental
import hep.dataforge.names.Name
import hep.dataforge.names.matches
import hep.dataforge.names.toName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf


/**
 * Check if data could be safely cast to given class
 */
private fun <R : Any> Data<*>.canCast(type: KType): Boolean = this.type.isSubtypeOf(type)

/**
 * Cast the node to given type if the cast is possible or return null
 */
@Suppress("UNCHECKED_CAST")
private fun <R : Any> Data<*>.castOrNull(type: KType): Data<R>? =
    if (!canCast<R>(type)) null else object : Data<R> by (this as Data<R>) {
        override val type: KType = type
    }

/**
 * Unsafe cast of data node
 */
private fun <R : Any> Data<*>.cast(type: KType): Data<R> =
    castOrNull(type) ?: error("Can't cast ${this.type} to $type")

private inline fun <reified R : Any> Data<*>.cast(): Data<R> = cast(typeOf<R>())

@Suppress("UNCHECKED_CAST")
private fun <R : Any> DataSet<*>.castOrNull(type: KType): DataSet<R>? =
    if (!canCast<R>(type)) null else object : DataSet<R> by (this as DataSet<R>) {
        override val dataType: KType = type
    }


private fun <R : Any> DataSet<*>.cast(type: KType): DataSet<R> =
    castOrNull(type) ?: error("Can't cast ${this.dataType} to $type")

/**
 * Check that node is compatible with given type meaning that each element could be cast to the type
 */
private fun <R : Any> DataSet<*>.canCast(type: KType): Boolean =
    type.isSubtypeOf(this.dataType)

/**
 * Select all data matching given type and filters. Does not modify paths
 */
@OptIn(DFExperimental::class)
@PublishedApi
internal fun <R : Any> DataSet<*>.select(
    type: KType,
    namePattern: Name? = null,
): ActiveDataSet<R> = object : ActiveDataSet<R> {
    override val dataType = type

    @Suppress("UNCHECKED_CAST")
    override fun flow(): Flow<NamedData<R>> = this@select.flow().filter {
        it.type.isSubtypeOf(type) && (namePattern == null || it.name.matches(namePattern))
    }.map {
        it as NamedData<R>
    }

    override suspend fun getData(name: Name): Data<R>? = this@select.getData(name)?.castOrNull(type)

    override val updates: Flow<Name> = this@select.updates.filter {
        val datum = this@select.getData(it)
        datum?.canCast<R>(type) ?: false
    }

}

/**
 * Select a single datum of the appropriate type
 */
public inline fun <reified R : Any> DataSet<*>.select(namePattern: Name? = null): DataSet<R> =
    select(typeOf<R>(), namePattern)

public suspend fun <R : Any> DataSet<*>.selectOne(type: KType, name: Name): NamedData<R>? =
    getData(name)?.castOrNull<R>(type)?.named(name)

public suspend inline fun <reified R : Any> DataSet<*>.selectOne(name: Name): NamedData<R>? = selectOne(typeOf<R>(), name)

public suspend inline fun <reified R : Any> DataSet<*>.selectOne(name: String): NamedData<R>? =
    selectOne(typeOf<R>(), name.toName())