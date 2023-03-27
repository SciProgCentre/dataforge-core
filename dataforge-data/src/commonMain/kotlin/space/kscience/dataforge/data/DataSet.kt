package space.kscience.dataforge.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapNotNull
import space.kscience.dataforge.data.Data.Companion.TYPE_OF_NOTHING
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.set
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.endsWith
import space.kscience.dataforge.names.parseAsName
import kotlin.reflect.KType

public interface DataSet<out T : Any> {

    /**
     * The minimal common ancestor to all data in the node
     */
    public val dataType: KType

    /**
     * Meta-data associated with this node. If no meta is provided, returns [Meta.EMPTY].
     */
    public val meta: Meta

    /**
     * Traverse this [DataSet] returning named data instances. The order is not guaranteed.
     */
    public operator fun iterator(): Iterator<NamedData<T>>

    /**
     * Get data with given name.
     */
    public operator fun get(name: Name): Data<T>?

    public companion object {
        public val META_KEY: Name = "@meta".asName()

        /**
         * An empty [DataSet] that suits all types
         */
        public val EMPTY: DataSet<Nothing> = object : DataSet<Nothing> {
            override val dataType: KType = TYPE_OF_NOTHING
            override val meta: Meta get() = Meta.EMPTY

            override fun iterator(): Iterator<NamedData<Nothing>> = emptySequence<NamedData<Nothing>>().iterator()

            override fun get(name: Name): Data<Nothing>? = null
        }
    }
}

public fun <T : Any> DataSet<T>.asSequence(): Sequence<NamedData<T>> = object : Sequence<NamedData<T>> {
    override fun iterator(): Iterator<NamedData<T>> = this@asSequence.iterator()
}

/**
 * Return a single [Data] in this [DataSet]. Throw error if it is not single.
 */
public fun <T : Any> DataSet<T>.single(): NamedData<T> = asSequence().single()

public fun <T : Any> DataSet<T>.asIterable(): Iterable<NamedData<T>> = object : Iterable<NamedData<T>> {
    override fun iterator(): Iterator<NamedData<T>> = this@asIterable.iterator()
}

public operator fun <T : Any> DataSet<T>.get(name: String): Data<T>? = get(name.parseAsName())

/**
 * A [DataSet] with propagated updates.
 */
public interface DataSource<out T : Any> : DataSet<T>, CoroutineScope {

    /**
     * A flow of updated item names. Updates are propagated in a form of [Flow] of names of updated nodes.
     * Those can include new data items and replacement of existing ones. The replaced items could update existing data content
     * and replace it completely, so they should be pulled again.
     *
     */
    public val updates: Flow<Name>

    /**
     * Stop generating updates from this [DataSource]
     */
    public fun close() {
        coroutineContext[Job]?.cancel()
    }
}

public val <T : Any> DataSet<T>.updates: Flow<Name> get() = if (this is DataSource) updates else emptyFlow()
//
///**
// * Flow all data nodes with names starting with [branchName]
// */
//public fun <T : Any> DataSet<T>.children(branchName: Name): Sequence<NamedData<T>> =
//    this@children.asSequence().filter {
//        it.name.startsWith(branchName)
//    }

/**
 * Start computation for all goals in data node and return a job for the whole node
 */
public fun <T : Any> DataSet<T>.startAll(coroutineScope: CoroutineScope): Job = coroutineScope.launch {
    asIterable().map {
        it.launch(this@launch)
    }.joinAll()
}

public suspend fun <T : Any> DataSet<T>.computeAndJoinAll(): Unit = coroutineScope { startAll(this).join() }

public fun DataSet<*>.toMeta(): Meta = Meta {
    forEach {
        if (it.name.endsWith(DataSet.META_KEY)) {
            set(it.name, it.meta)
        } else {
            it.name put {
                "type" put it.type.toString()
                "meta" put it.meta
            }
        }
    }
}

public val <T : Any> DataSet<T>.updatesWithData: Flow<NamedData<T>> get() = updates.mapNotNull { get(it)?.named(it) }