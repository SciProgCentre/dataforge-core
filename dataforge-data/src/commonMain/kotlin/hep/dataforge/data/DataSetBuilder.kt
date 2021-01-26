package hep.dataforge.data

import hep.dataforge.actions.NamedData
import hep.dataforge.meta.DFExperimental
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.names.Name
import hep.dataforge.names.plus
import hep.dataforge.names.toName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

public interface DataSetBuilder<in T : Any> {
    /**
     * Remove all data items starting with [name]
     */
    public suspend fun remove(name: Name)

    public suspend fun emit(name: Name, data: Data<T>?)

    /**
     * Set a current state of given [dataSet] into a branch [name]. Does not propagate updates
     */
    public suspend fun emit(name: Name, dataSet: DataSet<T>) {
        //remove previous items
        if (name != Name.EMPTY) {
            remove(name)
        }

        //Set new items
        dataSet.flow().collect {
            emit(name + it.name, it.data)
        }
    }

    /**
     * Append data to node
     */
    public suspend infix fun String.put(data: Data<T>): Unit = emit(toName(), data)

    /**
     * Append node
     */
    public suspend infix fun String.put(dataSet: DataSet<T>): Unit = emit(toName(), dataSet)

    /**
     * Build and append node
     */
    public suspend infix fun String.put(block: suspend DataSetBuilder<T>.() -> Unit): Unit = emit(toName(), block)
}

private class SubSetBuilder<in T : Any>(private val parent: DataSetBuilder<T>, private val branch: Name) :
    DataSetBuilder<T> {
    override suspend fun remove(name: Name) {
        parent.remove(branch + name)
    }

    override suspend fun emit(name: Name, data: Data<T>?) {
        parent.emit(branch + name, data)
    }

    override suspend fun emit(name: Name, dataSet: DataSet<T>) {
        parent.emit(branch + name, dataSet)
    }
}

public suspend fun <T : Any> DataSetBuilder<T>.emit(name: Name, block: suspend DataSetBuilder<T>.() -> Unit) {
    SubSetBuilder(this, name).apply { block() }
}


public suspend fun <T : Any> DataSetBuilder<T>.emit(name: String, data: Data<T>) {
    emit(name.toName(), data)
}

public suspend fun <T : Any> DataSetBuilder<T>.data(name: Name, data: T, meta: Meta = Meta.EMPTY) {
    emit(name, Data.static(data, meta))
}

public suspend fun <T : Any> DataSetBuilder<T>.data(name: Name, data: T, block: MetaBuilder.() -> Unit = {}) {
    emit(name, Data.static(data, Meta(block)))
}

public suspend fun <T : Any> DataSetBuilder<T>.data(name: String, data: T, block: MetaBuilder.() -> Unit = {}) {
    emit(name.toName(), Data.static(data, Meta(block)))
}

public suspend fun <T : Any> DataSetBuilder<T>.emit(name: String, set: DataSet<T>) {
    this.emit(name.toName(), set)
}

public suspend fun <T : Any> DataSetBuilder<T>.emit(name: String, block: suspend DataSetBuilder<T>.() -> Unit): Unit =
    this@emit.emit(name.toName(), block)

public suspend fun <T : Any> DataSetBuilder<T>.emit(data: NamedData<T>) {
    emit(data.name, data.data)
}

/**
 * Update data with given node data and meta with node meta.
 */
@DFExperimental
public suspend fun <T : Any> DataSetBuilder<T>.populate(tree: DataSet<T>): Unit = coroutineScope {
    tree.flow().collect {
        //TODO check if the place is occupied
        emit(it.name, it.data)
    }
}

public suspend fun <T : Any> DataSetBuilder<T>.populate(flow: Flow<NamedData<T>>) {
    flow.collect {
        emit(it.name, it.data)
    }
}
