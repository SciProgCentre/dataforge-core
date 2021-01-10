package hep.dataforge.data

import hep.dataforge.names.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlin.reflect.KClass

private class StaticDataTree<T : Any>(
    override val dataType: KClass<out T>,
) : DataSetBuilder<T>, DataTree<T> {

    private val items: MutableMap<NameToken, DataTreeItem<T>> = HashMap()

    override val updates: Flow<Name> = emptyFlow()

    override suspend fun items(): Map<NameToken, DataTreeItem<T>> = items

    override fun remove(name: Name) {
        when (name.length) {
            0 -> error("Can't remove root tree node")
            1 -> items.remove(name.firstOrNull()!!)
            else -> (items[name.firstOrNull()!!].tree as? StaticDataTree<T>)?.remove(name.cutFirst())
        }
    }

    fun getOrCreateNode(name: Name): StaticDataTree<T> = when (name.length) {
        0 -> this
        1 -> {
            val itemName = name.firstOrNull()!!
            (items[itemName].tree as? StaticDataTree<T>) ?: StaticDataTree(dataType).also {
                items[itemName] = DataTreeItem.Node(it)
            }
        }
        else -> getOrCreateNode(name.cutLast()).getOrCreateNode(name.lastOrNull()!!.asName())
    }

    private operator fun set(name: Name, item: DataTreeItem<T>?) {
        if (name.isEmpty()) error("Can't set top level tree node")
        if (item == null) {
            remove(name)
        } else {
            getOrCreateNode(name.cutLast()).items[name.lastOrNull()!!] = item
        }
    }

    override fun set(name: Name, data: Data<T>?) {
        set(name, data?.let { DataTreeItem.Leaf(it) })
    }

    override suspend fun set(name: Name, dataSet: DataSet<T>) {
        if (dataSet is StaticDataTree) {
            set(name, DataTreeItem.Node(dataSet))
        } else {
            coroutineScope {
                dataSet.flow().collect {
                    set(name + it.name, it.data)
                }
            }
        }
    }

    override fun set(name: Name, block: DataSetBuilder<T>.() -> Unit) {
        val tree = StaticDataTree(dataType).apply(block)
        set(name, DataTreeItem.Node(tree))
    }
}

public fun <T : Any> DataTree.Companion.static(
    dataType: KClass<out T>,
    block: DataSetBuilder<T>.() -> Unit,
): DataTree<T> = StaticDataTree(dataType).apply(block)

public inline fun <reified T : Any> DataTree.Companion.static(
    noinline block: DataSetBuilder<T>.() -> Unit,
): DataTree<T> = static(T::class, block)

public suspend fun <T : Any> DataSet<T>.toStaticTree(): DataTree<T> = StaticDataTree(dataType).apply {
    update(this@toStaticTree)
}