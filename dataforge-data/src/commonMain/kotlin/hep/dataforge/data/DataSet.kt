package hep.dataforge.data

import hep.dataforge.meta.DFExperimental
import hep.dataforge.names.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.reflect.KClass

public interface DataSet<out T : Any> {

    /**
     * The minimal common ancestor to all data in the node
     */
    public val dataType: KClass<out T>

    /**
     * Traverse this provider or its child. The order is not guaranteed.
     * [root] points to a root name for traversal. If it is empty, traverse this source, if it points to a [Data],
     * return flow, that contains single [Data], if it points to a node with children, return children.
     */
    public fun flow(): Flow<NamedData<T>>

    /**
     * Get data with given name.
     */
    public suspend fun getData(name: Name): Data<T>?

    /**
     * Get a snapshot of names of children of given node. Empty if node does not exist or is a leaf.
     *
     * By default traverses the whole tree. Could be optimized in descendants
     */
    public suspend fun listChildren(prefix: Name = Name.EMPTY): List<Name> =
        flow().map { it.name }.filter { it.startsWith(prefix) && (it.length == prefix.length + 1) }.toList()

    /**
     * A flow of updated item names. Updates are propagated in a form of [Flow] of names of updated nodes.
     * Those can include new data items and replacement of existing ones. The replaced items could update existing data content
     * and replace it completely, so they should be pulled again.
     *
     */
    public val updates: Flow<Name>

    public companion object {
        public val META_KEY: Name = "@meta".asName()
    }
}

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
 * Flow all data nodes with names starting with [branchName]
 */
public fun <T : Any> DataSet<T>.flowChildren(branchName: Name): Flow<NamedData<T>> = this@flowChildren.flow().filter {
    it.name.startsWith(branchName)
}

/**
 * Get a subset of data starting with a given [branchName]
 */
public fun <T : Any> DataSet<T>.branch(branchName: Name): DataSet<T> = if (branchName.isEmpty()) this
else object : DataSet<T> {
    override val dataType: KClass<out T> get() = this@branch.dataType

    override fun flow(): Flow<NamedData<T>> = this@branch.flow().mapNotNull {
        it.name.removeHeadOrNull(branchName)?.let { name ->
            it.data.named(name)
        }
    }

    override suspend fun getData(name: Name): Data<T>? = this@branch.getData(branchName + name)

    override val updates: Flow<Name> get() = this@branch.updates.mapNotNull { it.removeHeadOrNull(branchName) }
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
 * Start computation for all goals in data node and return a job for the whole node
 */
public fun <T : Any> DataSet<T>.startAll(coroutineScope: CoroutineScope): Job = coroutineScope.launch {
    flow().map {
        it.launch(this@launch)
    }.toList().joinAll()
}

public suspend fun <T : Any> DataSet<T>.join(): Unit = coroutineScope { startAll(this).join() }