package space.kscience.dataforge.data

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.plus
import kotlin.reflect.KType

public interface DataSetBuilder<in T : Any> {
    public val dataType: KType

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
    public suspend infix fun String.put(data: Data<T>): Unit = emit(Name.parse(this), data)

    /**
     * Append node
     */
    public suspend infix fun String.put(dataSet: DataSet<T>): Unit = emit(Name.parse(this), dataSet)

    /**
     * Build and append node
     */
    public suspend infix fun String.put(block: suspend DataSetBuilder<T>.() -> Unit): Unit = emit(Name.parse(this), block)
}

private class SubSetBuilder<in T : Any>(
    private val parent: DataSetBuilder<T>,
    private val branch: Name,
) : DataSetBuilder<T> {
    override val dataType: KType get() = parent.dataType

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
    emit(Name.parse(name), data)
}

public suspend fun <T : Any> DataSetBuilder<T>.emit(name: String, set: DataSet<T>) {
    this.emit(Name.parse(name), set)
}

public suspend fun <T : Any> DataSetBuilder<T>.emit(name: String, block: suspend DataSetBuilder<T>.() -> Unit): Unit =
    this@emit.emit(Name.parse(name), block)

public suspend fun <T : Any> DataSetBuilder<T>.emit(data: NamedData<T>) {
    emit(data.name, data.data)
}

/**
 * Produce lazy [Data] and emit it into the [DataSetBuilder]
 */
public suspend inline fun <reified T : Any> DataSetBuilder<T>.produce(
    name: String,
    meta: Meta = Meta.EMPTY,
    noinline producer: suspend () -> T,
) {
    val data = Data(meta, block = producer)
    emit(name, data)
}

public suspend inline fun <reified T : Any> DataSetBuilder<T>.produce(
    name: Name,
    meta: Meta = Meta.EMPTY,
    noinline producer: suspend () -> T,
) {
    val data = Data(meta, block = producer)
    emit(name, data)
}

/**
 * Emit a static data with the fixed value
 */
public suspend inline fun <reified T : Any> DataSetBuilder<T>.static(
    name: String,
    data: T,
    meta: Meta = Meta.EMPTY
): Unit =
    emit(name, Data.static(data, meta))

public suspend inline fun <reified T : Any> DataSetBuilder<T>.static(
    name: Name,
    data: T,
    meta: Meta = Meta.EMPTY
): Unit =
    emit(name, Data.static(data, meta))

public suspend inline fun <reified T : Any> DataSetBuilder<T>.static(
    name: String,
    data: T,
    mutableMeta: MutableMeta.() -> Unit,
): Unit = emit(Name.parse(name), Data.static(data, Meta(mutableMeta)))

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
