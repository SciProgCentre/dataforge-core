package space.kscience.dataforge.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.*
import kotlin.contracts.contract
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A generic data provider
 */
public interface DataSource<out T> {

    /**
     * The minimal common ancestor to all data in the node
     */
    public val dataType: KType

    /**
     * Get data with given name. Or null if it is not present
     */
    public fun read(name: Name): Data<T>?
}

/**
 * A data provider with possible dynamic updates
 */
public interface ObservableDataSource<out T> : DataSource<T> {

    /**
     * Flow updates made to the data. Updates are considered critical. The producer will suspend unless all updates are consumed.
     */
    public val updates: Flow<DataUpdate<T>>
}

public suspend fun <T> ObservableDataSource<T>.awaitData(name: Name): Data<T> {
    return read(name) ?: updates.first { it.name == name && it.data != null }.data!!
}

public suspend fun <T> ObservableDataSource<T>.awaitData(name: String): Data<T> =
    awaitData(name.parseAsName())

/**
 * A tree like structure for data holding
 */
public interface DataTree<out T> : ObservableDataSource<T> {

    public val data: Data<T>?
    public val items: Map<NameToken, DataTree<T>>

    override fun read(name: Name): Data<T>? = when (name.length) {
        0 -> data
        else -> items[name.first()]?.read(name.cutFirst())
    }

    /**
     * Flow updates made to the data
     */
    override val updates: Flow<DataUpdate<T>>

    public companion object {
        private object EmptyDataTree : DataTree<Nothing> {
            override val data: Data<Nothing>? = null
            override val items: Map<NameToken, EmptyDataTree> = emptyMap()
            override val dataType: KType = typeOf<Unit>()

            override fun read(name: Name): Data<Nothing>? = null
            override val updates: Flow<DataUpdate<Nothing>> get() = emptyFlow()
        }

        public val EMPTY: DataTree<Nothing> = EmptyDataTree
    }
}

/**
 * An alias for easier access to tree values
 */
public operator fun <T> DataTree<T>.get(name: Name): Data<T>? = read(name)

public operator fun <T> DataTree<T>.get(name: String): Data<T>? = read(name.parseAsName())

/**
 * Return a sequence of all data items in this tree.
 * This method does not take updates into account.
 */
public fun <T> DataTree<T>.asSequence(
    namePrefix: Name = Name.EMPTY,
): Sequence<NamedData<T>> = sequence {
    data?.let { yield(it.named(namePrefix)) }
    items.forEach { (token, tree) ->
        yieldAll(tree.asSequence(namePrefix + token))
    }
}

/**
 * Walk the data tree depth-first.
 *
 * @return a [Sequence] of pairs [Name]-[DataTree] for all nodes including the root one.
 */
public fun <T> DataTree<T>.walk(
    namePrefix: Name = Name.EMPTY,
): Sequence<Pair<Name, DataTree<T>>> = sequence {
    yield(namePrefix to this@walk)
    items.forEach { (token, tree) ->
        yieldAll(tree.walk(namePrefix + token))
    }
}

public val DataTree<*>.meta: Meta? get() = data?.meta

/**
 * Provide subtree if it exists
 */
public tailrec fun <T> DataTree<T>.branch(name: Name): DataTree<T>? =
    when (name.length) {
        0 -> this
        1 -> items[name.first()]
        else -> items[name.first()]?.branch(name.cutFirst())
    }

public fun <T> DataTree<T>.branch(name: String): DataTree<T>? =
    branch(name.parseAsName())

public fun DataTree<*>.isEmpty(): Boolean = data == null && items.isEmpty()

/**
 * Check if the [DataTree] is observable
 */
public fun <T> DataSource<T>.isObservable(): Boolean {
    contract {
        returns(true) implies (this@isObservable is ObservableDataSource<T>)
    }
    return this is ObservableDataSource<T>
}

