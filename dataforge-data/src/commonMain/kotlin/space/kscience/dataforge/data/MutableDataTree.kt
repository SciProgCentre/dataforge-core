package space.kscience.dataforge.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.mapNotNull
import space.kscience.dataforge.misc.UnsafeKType
import space.kscience.dataforge.names.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A marker scope for data builders
 */
public interface DataBuilderScope<in T> {
    public companion object : DataBuilderScope<Nothing>
}

@Suppress("UNCHECKED_CAST")
public fun <T> DataBuilderScope(): DataBuilderScope<T> = DataBuilderScope as DataBuilderScope<T>


/**
 * A mutable version of [DataTree]
 */
public interface MutableDataTree<T> : DataTree<T>, DataSink<T> {
    override val items: Map<NameToken, MutableDataTree<T>>
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
    override val updates = MutableSharedFlow<Name>()

    inner class MutableDataTreeBranch(val branchName: Name) : MutableDataTree<T> {

        override var data: Data<T>? = null
            private set

        override val items = HashMap<NameToken, MutableDataTree<T>>()

        override val updates: Flow<Name> = this@MutableDataTreeRoot.updates.mapNotNull { update ->
            update.removeFirstOrNull(branchName)
        }
        override val dataType: KType get() = this@MutableDataTreeRoot.dataType

        override suspend fun write(
            name: Name,
            data: Data<T>?
        ) {
            when (name.length) {
                0 -> {
                    this.data = data
                    this@MutableDataTreeRoot.updates.emit(branchName)
                }

                else -> {
                    val token = name.first()
                    items.getOrPut(token) { MutableDataTreeBranch(branchName + token) }.write(name.cutFirst(), data)
                }
            }
        }
    }
    override var data: Data<T>? = null
        private set

    override suspend fun write(
        name: Name,
        data: Data<T>?
    ) {
        when (name.length) {
            0 -> {
                this.data = data
                this@MutableDataTreeRoot.updates.emit(Name.EMPTY)
            }

            else -> {
                val token = name.first()
                items.getOrPut(token) { MutableDataTreeBranch(token.asName()) }.write(name.cutFirst(), data)
            }
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
 * Create and initialize an observable mutable data tree.
 */
@OptIn(UnsafeKType::class)
public inline fun <reified T> MutableDataTree(
    generator: MutableDataTree<T>.() -> Unit = {},
): MutableDataTree<T> = MutableDataTree<T>(typeOf<T>()).apply { generator() }