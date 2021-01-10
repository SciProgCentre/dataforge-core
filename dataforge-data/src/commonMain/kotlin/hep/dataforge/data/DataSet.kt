package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.meta.set
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
 * Flow all data nodes with names starting with [branchName]
 */
public fun <T : Any> DataSet<T>.flowChildren(branchName: Name): Flow<NamedData<T>> = this@flowChildren.flow().filter {
    it.name.startsWith(branchName)
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

public suspend fun DataSet<*>.toMeta(): Meta = Meta {
    flow().collect {
        if (it.name.endsWith(DataSet.META_KEY)) {
            set(it.name, it.meta)
        } else {
            it.name put {
                "type" put it.type.simpleName
                "meta" put it.meta
            }
        }
    }
}