package space.kscience.dataforge.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.mapNotNull
import space.kscience.dataforge.misc.UnsafeKType
import space.kscience.dataforge.names.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public interface DataBuilderScope<in T> {
    public companion object : DataBuilderScope<Nothing>
}

@Suppress("UNCHECKED_CAST")
public fun <T> DataBuilderScope(): DataBuilderScope<T> = DataBuilderScope as DataBuilderScope<T>

/**
 * Asynchronous data sink
 */
public fun interface DataSink<in T> : DataBuilderScope<T> {
    /**
     * Put data and notify listeners if needed
     */
    public suspend fun put(name: Name, data: Data<T>?)
}


/**
 * A mutable version of [DataTree]
 */
public interface MutableDataTree<T> : DataTree<T>, DataSink<T> {
    override var data: Data<T>?

    override val items: Map<NameToken, MutableDataTree<T>>

    public fun getOrCreateItem(token: NameToken): MutableDataTree<T>

    public suspend fun put(token: NameToken, data: Data<T>?)

    override suspend fun put(name: Name, data: Data<T>?): Unit {
        when (name.length) {
            0 -> this.data = data
            1 -> put(name.first(), data)
            else -> getOrCreateItem(name.first()).put(name.cutFirst(), data)
        }
    }
}

/**
 * Provide a mutable subtree if it exists
 */
public tailrec fun <T> MutableDataTree<T>.branch(name: Name): MutableDataTree<T>? =
    when (name.length) {
        0 -> this
        1 -> items[name.first()]
        else -> items[name.first()]?.branch(name.cutFirst())
    }

private class MutableDataTreeRoot<T>(
    override val dataType: KType,
) : MutableDataTree<T> {

    override val items = HashMap<NameToken, MutableDataTree<T>>()
    override val updates = MutableSharedFlow<Name>(extraBufferCapacity = 100)

    inner class MutableDataTreeBranch(val branchName: Name) : MutableDataTree<T> {

        override var data: Data<T>? = null

        override val items = HashMap<NameToken, MutableDataTree<T>>()

        override val updates: Flow<Name> = this@MutableDataTreeRoot.updates.mapNotNull { update ->
            update.removeFirstOrNull(branchName)
        }
        override val dataType: KType get() = this@MutableDataTreeRoot.dataType


        override fun getOrCreateItem(token: NameToken): MutableDataTree<T> =
            items.getOrPut(token) { MutableDataTreeBranch(branchName + token) }

        override suspend fun put(token: NameToken, data: Data<T>?) {
            this.data = data
            this@MutableDataTreeRoot.updates.emit(branchName + token)
        }
    }

    override var data: Data<T>? = null

    override fun getOrCreateItem(token: NameToken): MutableDataTree<T> = items.getOrPut(token) {
        MutableDataTreeBranch(token.asName())
    }

    override suspend fun put(token: NameToken, data: Data<T>?) {
        this.data = data
        updates.emit(token.asName())
    }
}

/**
 * Create a new [MutableDataTree]
 */
@UnsafeKType
public fun <T> MutableDataTree(
    type: KType,
): MutableDataTree<T> = MutableDataTreeRoot<T>(type)

/**
 * Create and initialize a observable mutable data tree.
 */
@OptIn(UnsafeKType::class)
public inline fun <reified T> MutableDataTree(
    generator: MutableDataTree<T>.() -> Unit = {},
): MutableDataTree<T> = MutableDataTree<T>(typeOf<T>()).apply { generator() }