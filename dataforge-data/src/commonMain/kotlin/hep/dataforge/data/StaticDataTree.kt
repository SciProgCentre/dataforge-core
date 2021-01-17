package hep.dataforge.data

import hep.dataforge.names.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlin.reflect.KClass

@PublishedApi
internal class StaticDataTree<T : Any>(
    override val dataType: KClass<out T>,
) : DataSetBuilder<T>, DataTree<T> {

    private val items: MutableMap<NameToken, DataTreeItem<T>> = HashMap()

    override suspend fun items(): Map<NameToken, DataTreeItem<T>> = items.filter { !it.key.body.startsWith("@") }

    override suspend fun remove(name: Name) {
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

    private suspend fun set(name: Name, item: DataTreeItem<T>?) {
        if (name.isEmpty()) error("Can't set top level tree node")
        if (item == null) {
            remove(name)
        } else {
            getOrCreateNode(name.cutLast()).items[name.lastOrNull()!!] = item
        }
    }

    override suspend fun set(name: Name, data: Data<T>?) {
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
}

public suspend fun <T : Any> DataTree(
    dataType: KClass<out T>,
    block: suspend DataSetBuilder<T>.() -> Unit,
): DataTree<T> = StaticDataTree(dataType).apply { block() }

public suspend inline fun <reified T : Any> DataTree(
    noinline block: suspend DataSetBuilder<T>.() -> Unit,
): DataTree<T> = DataTree(T::class, block)

public suspend fun <T : Any> DataSet<T>.seal(): DataTree<T> = DataTree(dataType){
    update(this@seal)
}