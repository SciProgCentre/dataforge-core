package space.kscience.dataforge.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.matches
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf


/**
 * Cast the node to given type if the cast is possible or return null
 */
@Suppress("UNCHECKED_CAST")
private fun <R : Any> Data<*>.castOrNull(type: KType): Data<R>? =
    if (!this.type.isSubtypeOf(type)) {
        null
    } else {
        object : Data<R> by (this as Data<R>) {
            override val type: KType = type
        }
    }

/**
 * Select all data matching given type and filters. Does not modify paths
 *
 * @param namePattern a name match patter according to [Name.matches]
 * @param filter addition filtering condition based on item name and meta. By default, accepts all
 */
@OptIn(DFExperimental::class)
public fun <R : Any> DataSet<*>.select(
    type: KType,
    namePattern: Name? = null,
    filter: (name: Name, meta: Meta) -> Boolean = { _, _ -> true }
): ActiveDataSet<R> = object : ActiveDataSet<R> {
    override val dataType = type

    private fun checkDatum(name: Name, datum: Data<*>): Boolean = datum.type.isSubtypeOf(type)
            && (namePattern == null || name.matches(namePattern))
            && filter(name, datum.meta)

    override fun flow(): Flow<NamedData<R>> = this@select.flow().filter {
        checkDatum(it.name, it.data)
    }.map {
        @Suppress("UNCHECKED_CAST")
        it as NamedData<R>
    }

    override suspend fun getData(name: Name): Data<R>? = this@select.getData(name)?.let { datum ->
        if (checkDatum(name, datum)) datum.castOrNull(type) else null
    }

    override val updates: Flow<Name> = this@select.updates.filter {
        val datum = this@select.getData(it) ?: return@filter false
        checkDatum(it, datum)
    }
}

/**
 * Select a single datum of the appropriate type
 */
public inline fun <reified R : Any> DataSet<*>.select(
    namePattern: Name? = null,
    noinline filter: (name: Name, meta: Meta) -> Boolean = { _, _ -> true }
): DataSet<R> = select(typeOf<R>(), namePattern, filter)

/**
 * Select a single datum if it is present and of given [type]
 */
public suspend fun <R : Any> DataSet<*>.selectOne(type: KType, name: Name): NamedData<R>? =
    getData(name)?.castOrNull<R>(type)?.named(name)

public suspend inline fun <reified R : Any> DataSet<*>.selectOne(name: Name): NamedData<R>? =
    selectOne(typeOf<R>(), name)

public suspend inline fun <reified R : Any> DataSet<*>.selectOne(name: String): NamedData<R>? =
    selectOne(typeOf<R>(), Name.parse(name))