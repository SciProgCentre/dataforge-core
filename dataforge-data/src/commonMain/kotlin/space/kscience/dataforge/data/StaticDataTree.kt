package space.kscience.dataforge.data

import kotlinx.coroutines.coroutineScope
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.cutFirst
import space.kscience.dataforge.names.cutLast
import space.kscience.dataforge.names.firstOrNull
import space.kscience.dataforge.names.isEmpty
import space.kscience.dataforge.names.lastOrNull
import space.kscience.dataforge.names.length
import space.kscience.dataforge.names.plus
import kotlin.collections.set
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@PublishedApi
internal class StaticDataTree<T : Any>(
    override val dataType: KType,
) : DataSetBuilder<T>, DataTree<T> {

    private val _items: MutableMap<NameToken, DataTreeItem<T>> = HashMap()

    override val items: Map<NameToken, DataTreeItem<T>>
        get() = _items.filter { !it.key.body.startsWith("@") }

    override fun remove(name: Name) {
        when (name.length) {
            0 -> error("Can't remove root tree node")
            1 -> _items.remove(name.firstOrNull()!!)
            else -> (_items[name.firstOrNull()!!].tree as? StaticDataTree<T>)?.remove(name.cutFirst())
        }
    }

    private fun getOrCreateNode(name: Name): StaticDataTree<T> = when (name.length) {
        0 -> this
        1 -> {
            val itemName = name.firstOrNull()!!
            (_items[itemName].tree as? StaticDataTree<T>) ?: StaticDataTree<T>(dataType).also {
                _items[itemName] = DataTreeItem.Node(it)
            }
        }
        else -> getOrCreateNode(name.cutLast()).getOrCreateNode(name.lastOrNull()!!.asName())
    }

    private fun set(name: Name, item: DataTreeItem<T>?) {
        if (name.isEmpty()) error("Can't set top level tree node")
        if (item == null) {
            remove(name)
        } else {
            getOrCreateNode(name.cutLast())._items[name.lastOrNull()!!] = item
        }
    }

    override fun data(name: Name, data: Data<T>?) {
        set(name, data?.let { DataTreeItem.Leaf(it) })
    }

    override fun node(name: Name, dataSet: DataSet<T>) {
        if (dataSet is StaticDataTree) {
            set(name, DataTreeItem.Node(dataSet))
        } else {
            dataSet.forEach {
                data(name + it.name, it.data)
            }
        }
    }

    override fun meta(name: Name, meta: Meta) {
        val item = getItem(name)
        if (item is DataTreeItem.Leaf) TODO("Can't change meta of existing leaf item.")
        data(name + DataTree.META_ITEM_NAME_TOKEN, Data.empty(meta))
    }
}

@Suppress("FunctionName")
public inline fun <T : Any> DataTree(
    dataType: KType,
    block: DataSetBuilder<T>.() -> Unit,
): DataTree<T> = StaticDataTree<T>(dataType).apply { block() }

@Suppress("FunctionName")
public inline fun <reified T : Any> DataTree(
    noinline block: DataSetBuilder<T>.() -> Unit = {},
): DataTree<T> = DataTree(typeOf<T>(), block)

@OptIn(DFExperimental::class)
public fun <T : Any> DataSet<T>.seal(): DataTree<T> = DataTree(dataType) {
    populateFrom(this@seal)
}
