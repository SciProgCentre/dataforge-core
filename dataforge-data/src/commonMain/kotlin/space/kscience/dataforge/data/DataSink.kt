package space.kscience.dataforge.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import space.kscience.dataforge.misc.UnsafeKType
import space.kscience.dataforge.names.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public interface DataSink<in T> {
    /**
     * Put data without notification
     */
    public fun put(name: Name, data: Data<T>?)

    /**
     * Put data and propagate changes downstream
     */
    public suspend fun update(name: Name, data: Data<T>?)
}

/**
 * Launch continuous update using
 */
public fun <T> DataSink<T>.launchUpdate(
    scope: CoroutineScope,
    updater: suspend DataSink<T>.() -> Unit,
): Job = scope.launch {
    object : DataSink<T> {
        override fun put(name: Name, data: Data<T>?) {
            launch {
                this@launchUpdate.update(name, data)
            }
        }

        override suspend fun update(name: Name, data: Data<T>?) {
            this@launchUpdate.update(name, data)
        }
    }.updater()
}

/**
 * A mutable version of [DataTree]
 */
public interface MutableDataTree<T> : DataTree<T>, DataSink<T> {
    override var data: Data<T>?

    override val items: Map<NameToken, MutableDataTree<T>>

    public fun getOrCreateItem(token: NameToken): MutableDataTree<T>

    public operator fun set(token: NameToken, data: Data<T>?)

    override fun put(name: Name, data: Data<T>?): Unit = set(name, data)
}

public tailrec operator fun <T> MutableDataTree<T>.set(name: Name, data: Data<T>?): Unit {
    when (name.length) {
        0 -> this.data = data
        1 -> set(name.first(), data)
        else -> getOrCreateItem(name.first())[name.cutFirst()] = data
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

    override val updates = MutableSharedFlow<DataUpdate<T>>(100, onBufferOverflow = BufferOverflow.DROP_LATEST)


    inner class MutableDataTreeBranch(val branchName: Name) : MutableDataTree<T> {

        override var data: Data<T>? = null

        override val items = HashMap<NameToken, MutableDataTree<T>>()

        override val updates: Flow<DataUpdate<T>> = this@MutableDataTreeRoot.updates.mapNotNull { update ->
            update.name.removeFirstOrNull(branchName)?.let {
                DataUpdate(update.data?.type ?: dataType, it, update.data)
            }
        }
        override val dataType: KType get() = this@MutableDataTreeRoot.dataType


        override fun getOrCreateItem(token: NameToken): MutableDataTree<T> =
            items.getOrPut(token) { MutableDataTreeBranch(branchName + token) }


        override fun set(token: NameToken, data: Data<T>?) {
            val subTree = getOrCreateItem(token)
            subTree.data = data
        }

        override suspend fun update(name: Name, data: Data<T>?) {
            if (name.isEmpty()) {
                this.data = data
                this@MutableDataTreeRoot.updates.emit(DataUpdate(data?.type ?: dataType, branchName + name, data))
            } else {
                getOrCreateItem(name.first()).update(name.cutFirst(), data)
            }
        }

    }


    override var data: Data<T>? = null

    override val items = HashMap<NameToken, MutableDataTree<T>>()

    override fun getOrCreateItem(token: NameToken): MutableDataTree<T> = items.getOrPut(token) {
        MutableDataTreeBranch(token.asName())
    }

    override fun set(token: NameToken, data: Data<T>?) {
        val subTree = getOrCreateItem(token)
        subTree.data = data
    }

    override suspend fun update(name: Name, data: Data<T>?) {
        if (name.isEmpty()) {
            this.data = data
            updates.emit(DataUpdate(data?.type ?: dataType, name, data))
        } else {
            getOrCreateItem(name.first()).update(name.cutFirst(), data)
        }
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