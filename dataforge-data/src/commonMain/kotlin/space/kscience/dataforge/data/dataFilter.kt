package space.kscience.dataforge.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.isEmpty
import space.kscience.dataforge.names.plus
import space.kscience.dataforge.names.removeHeadOrNull
import kotlin.reflect.KType


/**
 * A stateless filtered [DataSet]
 */
public fun <T : Any> DataSet<T>.filter(
    predicate: (Name, Data<T>) -> Boolean,
): ActiveDataSet<T> = object : ActiveDataSet<T> {

    override val dataType: KType get() = this@filter.dataType

    override val meta: Meta get() = this@filter.meta

    override fun flowData(): Flow<NamedData<T>> =
        this@filter.flowData().filter { predicate(it.name, it.data) }

    override fun get(name: Name): Data<T>? = this@filter.get(name)?.takeIf {
        predicate(name, it)
    }

    override val updates: Flow<Name> = this@filter.updates.filter flowFilter@{ name ->
        val theData = this@filter.get(name) ?: return@flowFilter false
        predicate(name, theData)
    }
}

/**
 * Generate a wrapper data set with a given name prefix appended to all names
 */
public fun <T : Any> DataSet<T>.withNamePrefix(prefix: Name): DataSet<T> = if (prefix.isEmpty()) this
else object : ActiveDataSet<T> {

    override val dataType: KType get() = this@withNamePrefix.dataType

    override val meta: Meta get() = this@withNamePrefix.meta


    override fun flowData(): Flow<NamedData<T>> = this@withNamePrefix.flowData().map { it.data.named(prefix + it.name) }

    override fun get(name: Name): Data<T>? =
        name.removeHeadOrNull(name)?.let { this@withNamePrefix.get(it) }

    override val updates: Flow<Name> get() = this@withNamePrefix.updates.map { prefix + it }
}

/**
 * Get a subset of data starting with a given [branchName]
 */
public fun <T : Any> DataSet<T>.branch(branchName: Name): DataSet<T> = if (branchName.isEmpty()) {
    this
} else object : ActiveDataSet<T> {
    override val dataType: KType get() = this@branch.dataType

    override val meta: Meta get() = this@branch.meta

    override fun flowData(): Flow<NamedData<T>> = this@branch.flowData().mapNotNull {
        it.name.removeHeadOrNull(branchName)?.let { name ->
            it.data.named(name)
        }
    }

    override fun get(name: Name): Data<T>? = this@branch.get(branchName + name)

    override val updates: Flow<Name> get() = this@branch.updates.mapNotNull { it.removeHeadOrNull(branchName) }
}

public fun <T : Any> DataSet<T>.branch(branchName: String): DataSet<T> = this@branch.branch(Name.parse(branchName))

@DFExperimental
public suspend fun <T : Any> DataSet<T>.rootData(): Data<T>? = get(Name.EMPTY)

