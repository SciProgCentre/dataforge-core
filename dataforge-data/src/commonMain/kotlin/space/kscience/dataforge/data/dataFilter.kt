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
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KType


/**
 * A stateless filtered [DataSet]
 */
public fun <T : Any> DataSet<T>.filter(
    predicate: (Name, Meta) -> Boolean,
): DataSource<T> = object : DataSource<T> {

    override val dataType: KType get() = this@filter.dataType

    override val coroutineContext: CoroutineContext
        get() = (this@filter as? DataSource)?.coroutineContext ?: EmptyCoroutineContext


    override val meta: Meta get() = this@filter.meta

    override fun dataSequence(): Sequence<NamedData<T>> =
        this@filter.dataSequence().filter { predicate(it.name, it.meta) }

    override fun get(name: Name): Data<T>? = this@filter.get(name)?.takeIf {
        predicate(name, it.meta)
    }

    override val updates: Flow<Name> = this@filter.updates.filter flowFilter@{ name ->
        val theData = this@filter[name] ?: return@flowFilter false
        predicate(name, theData.meta)
    }
}

/**
 * Generate a wrapper data set with a given name prefix appended to all names
 */
public fun <T : Any> DataSet<T>.withNamePrefix(prefix: Name): DataSet<T> = if (prefix.isEmpty()) {
    this
} else object : DataSource<T> {

    override val dataType: KType get() = this@withNamePrefix.dataType

    override val coroutineContext: CoroutineContext
        get() = (this@withNamePrefix as? DataSource)?.coroutineContext ?: EmptyCoroutineContext

    override val meta: Meta get() = this@withNamePrefix.meta


    override fun dataSequence(): Sequence<NamedData<T>> =
        this@withNamePrefix.dataSequence().map { it.data.named(prefix + it.name) }

    override fun get(name: Name): Data<T>? =
        name.removeHeadOrNull(name)?.let { this@withNamePrefix.get(it) }

    override val updates: Flow<Name> get() = this@withNamePrefix.updates.map { prefix + it }
}

/**
 * Get a subset of data starting with a given [branchName]
 */
public fun <T : Any> DataSet<T>.branch(branchName: Name): DataSet<T> = if (branchName.isEmpty()) {
    this
} else object : DataSource<T> {
    override val dataType: KType get() = this@branch.dataType

    override val coroutineContext: CoroutineContext
        get() = (this@branch as? DataSource)?.coroutineContext ?: EmptyCoroutineContext

    override val meta: Meta get() = this@branch.meta

    override fun dataSequence(): Sequence<NamedData<T>> = this@branch.dataSequence().mapNotNull {
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

