package space.kscience.dataforge.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.matches
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
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
 * @param predicate addition filtering condition based on item name and meta. By default, accepts all
 */
@OptIn(DFExperimental::class)
public fun <R : Any> DataSet<*>.filterIsInstance(
    type: KType,
    predicate: (name: Name, meta: Meta) -> Boolean = { _, _ -> true },
): DataSource<R> = object : DataSource<R> {
    override val dataType = type

    override val coroutineContext: CoroutineContext
        get() = (this@filterIsInstance as? DataSource)?.coroutineContext ?: EmptyCoroutineContext

    override val meta: Meta get() = this@filterIsInstance.meta

    private fun checkDatum(name: Name, datum: Data<*>): Boolean = datum.type.isSubtypeOf(type)
            && predicate(name, datum.meta)

    override fun dataSequence(): Sequence<NamedData<R>> = this@filterIsInstance.dataSequence().filter {
        checkDatum(it.name, it.data)
    }.map {
        @Suppress("UNCHECKED_CAST")
        it as NamedData<R>
    }

    override fun get(name: Name): Data<R>? = this@filterIsInstance[name]?.let { datum ->
        if (checkDatum(name, datum)) datum.castOrNull(type) else null
    }

    override val updates: Flow<Name> = this@filterIsInstance.updates.filter { name ->
        get(name)?.let { datum ->
            checkDatum(name, datum)
        } ?: false
    }
}

/**
 * Select a single datum of the appropriate type
 */
public inline fun <reified R : Any> DataSet<*>.filterIsInstance(
    noinline predicate: (name: Name, meta: Meta) -> Boolean = { _, _ -> true },
): DataSet<R> = filterIsInstance(typeOf<R>(), predicate)

/**
 * Select a single datum if it is present and of given [type]
 */
public fun <R : Any> DataSet<*>.selectOne(type: KType, name: Name): NamedData<R>? =
    get(name)?.castOrNull<R>(type)?.named(name)

public inline fun <reified R : Any> DataSet<*>.selectOne(name: Name): NamedData<R>? =
    selectOne(typeOf<R>(), name)

public inline fun <reified R : Any> DataSet<*>.selectOne(name: String): NamedData<R>? =
    selectOne(typeOf<R>(), Name.parse(name))