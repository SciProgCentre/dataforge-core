package hep.dataforge.data

import hep.dataforge.meta.DFExperimental
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect

public interface DataSetBuilder<in T : Any> {
    public fun remove(name: Name)

    public operator fun set(name: Name, data: Data<T>?)

    public suspend fun set(name: Name, dataSet: DataSet<T>)

    public operator fun set(name: Name, block: DataSetBuilder<T>.() -> Unit)

    /**
     * Append data to node
     */
    public infix fun String.put(data: Data<T>): Unit = set(toName(), data)

    /**
     * Append node
     */
    public suspend infix fun String.put(tree: DataSet<T>): Unit = set(toName(), tree)

    /**
     * Build and append node
     */
    public infix fun String.put(block: DataSetBuilder<T>.() -> Unit): Unit = set(toName(), block)

}


public operator fun <T : Any> DataSetBuilder<T>.set(name: String, data: Data<T>) {
    this@set[name.toName()] = data
}

public fun <T : Any> DataSetBuilder<T>.data(name: Name, data: T, meta: Meta = Meta.EMPTY) {
    set(name, Data.static(data, meta))
}

public fun <T : Any> DataSetBuilder<T>.data(name: Name, data: T, block: MetaBuilder.() -> Unit = {}) {
    set(name, Data.static(data, Meta(block)))
}

public fun <T : Any> DataSetBuilder<T>.data(name: String, data: T, block: MetaBuilder.() -> Unit = {}) {
    set(name.toName(), Data.static(data, Meta(block)))
}

public suspend fun <T : Any> DataSetBuilder<T>.set(name: String, set: DataSet<T>) {
    this.set(name.toName(), set)
}

public operator fun <T : Any> DataSetBuilder<T>.set(name: String, block: DataSetBuilder<T>.() -> Unit): Unit =
    this@set.set(name.toName(), block)


/**
 * Update data with given node data and meta with node meta.
 */
@DFExperimental
public suspend fun <T: Any> DataSetBuilder<T>.update(tree: DataSet<T>): Unit = coroutineScope{
    tree.flow().collect {
        //TODO check if the place is occupied
        set(it.name, it.data)
    }
}
