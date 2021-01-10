package hep.dataforge.data

import hep.dataforge.meta.DFExperimental
import hep.dataforge.names.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlin.reflect.KClass


/**
 * A stateless filtered [DataSet]
 */
@DFExperimental
public fun <T : Any> DataSet<T>.filter(
    predicate: suspend (Name, Data<T>) -> Boolean,
): DataSet<T> = object : DataSet<T> {
    override val dataType: KClass<out T> get() = this@filter.dataType

    override fun flow(): Flow<NamedData<T>> =
        this@filter.flow().filter { predicate(it.name, it.data) }

    override suspend fun getData(name: Name): Data<T>? = this@filter.getData(name)?.takeIf {
        predicate(name, it)
    }

    override val updates: Flow<Name> = this@filter.updates.filter flowFilter@{ name ->
        val theData = this@filter.getData(name) ?: return@flowFilter false
        predicate(name, theData)
    }
}


/**
 * Generate a wrapper data set with a given name prefix appended to all names
 */
public fun <T : Any> DataSet<T>.withNamePrefix(prefix: Name): DataSet<T> = if (prefix.isEmpty()) this
else object : DataSet<T> {
    override val dataType: KClass<out T> get() = this@withNamePrefix.dataType

    override fun flow(): Flow<NamedData<T>> = this@withNamePrefix.flow().map { it.data.named(prefix + it.name) }

    override suspend fun getData(name: Name): Data<T>? =
        name.removeHeadOrNull(name)?.let { this@withNamePrefix.getData(it) }

    override val updates: Flow<Name> get() = this@withNamePrefix.updates.map { prefix + it }
}


/**
 * Get a subset of data starting with a given [branchName]
 */
public operator fun <T : Any> DataSet<T>.get(branchName: Name): DataSet<T> = if (branchName.isEmpty()) this
else object : DataSet<T> {
    override val dataType: KClass<out T> get() = this@get.dataType

    override fun flow(): Flow<NamedData<T>> = this@get.flow().mapNotNull {
        it.name.removeHeadOrNull(branchName)?.let { name ->
            it.data.named(name)
        }
    }

    override suspend fun getData(name: Name): Data<T>? = this@get.getData(branchName + name)

    override val updates: Flow<Name> get() = this@get.updates.mapNotNull { it.removeHeadOrNull(branchName) }
}

public operator fun <T : Any> DataSet<T>.get(branchName: String): DataSet<T> = this@get.get(branchName.toName())

@DFExperimental
public suspend fun <T : Any> DataSet<T>.rootData(): Data<T>? = getData(Name.EMPTY)
