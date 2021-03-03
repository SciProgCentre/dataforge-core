package space.kscience.dataforge.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.*
import kotlin.reflect.KType


/**
 * A stateless filtered [DataSet]
 */
public fun <T : Any> DataSet<T>.filter(
    predicate: suspend (Name, Data<T>) -> Boolean,
): ActiveDataSet<T> = object : ActiveDataSet<T> {
    override val dataType: KType get() = this@filter.dataType

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
else object : ActiveDataSet<T> {
    override val dataType: KType get() = this@withNamePrefix.dataType

    override fun flow(): Flow<NamedData<T>> = this@withNamePrefix.flow().map { it.data.named(prefix + it.name) }

    override suspend fun getData(name: Name): Data<T>? =
        name.removeHeadOrNull(name)?.let { this@withNamePrefix.getData(it) }

    override val updates: Flow<Name> get() = this@withNamePrefix.updates.map { prefix + it }
}

/**
 * Get a subset of data starting with a given [branchName]
 */
public fun <T : Any> DataSet<T>.branch(branchName: Name): DataSet<T> = if (branchName.isEmpty()) {
    this
} else object : ActiveDataSet<T> {
    override val dataType: KType get() = this@branch.dataType

    override fun flow(): Flow<NamedData<T>> = this@branch.flow().mapNotNull {
        it.name.removeHeadOrNull(branchName)?.let { name ->
            it.data.named(name)
        }
    }

    override suspend fun getData(name: Name): Data<T>? = this@branch.getData(branchName + name)

    override val updates: Flow<Name> get() = this@branch.updates.mapNotNull { it.removeHeadOrNull(branchName) }
}

public fun <T : Any> DataSet<T>.branch(branchName: String): DataSet<T> = this@branch.branch(branchName.toName())

@DFExperimental
public suspend fun <T : Any> DataSet<T>.rootData(): Data<T>? = getData(Name.EMPTY)

