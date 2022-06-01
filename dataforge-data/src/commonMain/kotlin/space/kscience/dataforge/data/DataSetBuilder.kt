package space.kscience.dataforge.data

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.isEmpty
import space.kscience.dataforge.names.plus
import kotlin.reflect.KType

public interface DataSetBuilder<in T : Any> {
    public val dataType: KType

    /**
     * Remove all data items starting with [name]
     */
    public fun remove(name: Name)

    public fun data(name: Name, data: Data<T>?)

    /**
     * Set a current state of given [dataSet] into a branch [name]. Does not propagate updates
     */
    public fun node(name: Name, dataSet: DataSet<T>) {
        //remove previous items
        if (name != Name.EMPTY) {
            remove(name)
        }

        //Set new items
        dataSet.forEach {
            data(name + it.name, it.data)
        }
    }

    /**
     * Set meta for the given node
     */
    public fun meta(name: Name, meta: Meta)

}

/**
 * Define meta in this [DataSet]
 */
public fun <T : Any> DataSetBuilder<T>.meta(value: Meta): Unit = meta(Name.EMPTY, value)

/**
 * Define meta in this [DataSet]
 */
public fun <T : Any> DataSetBuilder<T>.meta(mutableMeta: MutableMeta.() -> Unit): Unit = meta(Meta(mutableMeta))

@PublishedApi
internal class SubSetBuilder<in T : Any>(
    private val parent: DataSetBuilder<T>,
    private val branch: Name,
) : DataSetBuilder<T> {
    override val dataType: KType get() = parent.dataType

    override fun remove(name: Name) {
        parent.remove(branch + name)
    }

    override fun data(name: Name, data: Data<T>?) {
        parent.data(branch + name, data)
    }

    override fun node(name: Name, dataSet: DataSet<T>) {
        parent.node(branch + name, dataSet)
    }

    override fun meta(name: Name, meta: Meta) {
        parent.meta(branch + name, meta)
    }
}

public inline fun <T : Any> DataSetBuilder<T>.node(
    name: Name,
    crossinline block: DataSetBuilder<T>.() -> Unit,
) {
    if (name.isEmpty()) block() else SubSetBuilder(this, name).block()
}


public fun <T : Any> DataSetBuilder<T>.data(name: String, value: Data<T>) {
    data(Name.parse(name), value)
}

public fun <T : Any> DataSetBuilder<T>.node(name: String, set: DataSet<T>) {
    node(Name.parse(name), set)
}

public inline fun <T : Any> DataSetBuilder<T>.node(
    name: String,
    crossinline block: DataSetBuilder<T>.() -> Unit,
): Unit = node(Name.parse(name), block)

public fun <T : Any> DataSetBuilder<T>.set(value: NamedData<T>) {
    data(value.name, value.data)
}

/**
 * Produce lazy [Data] and emit it into the [DataSetBuilder]
 */
public inline fun <reified T : Any> DataSetBuilder<T>.produce(
    name: String,
    meta: Meta = Meta.EMPTY,
    noinline producer: suspend () -> T,
) {
    val data = Data(meta, block = producer)
    data(name, data)
}

public inline fun <reified T : Any> DataSetBuilder<T>.produce(
    name: Name,
    meta: Meta = Meta.EMPTY,
    noinline producer: suspend () -> T,
) {
    val data = Data(meta, block = producer)
    data(name, data)
}

/**
 * Emit a static data with the fixed value
 */
public inline fun <reified T : Any> DataSetBuilder<T>.static(
    name: String,
    data: T,
    meta: Meta = Meta.EMPTY,
): Unit = data(name, Data.static(data, meta))

public inline fun <reified T : Any> DataSetBuilder<T>.static(
    name: Name,
    data: T,
    meta: Meta = Meta.EMPTY,
): Unit = data(name, Data.static(data, meta))

public inline fun <reified T : Any> DataSetBuilder<T>.static(
    name: String,
    data: T,
    mutableMeta: MutableMeta.() -> Unit,
): Unit = data(Name.parse(name), Data.static(data, Meta(mutableMeta)))

/**
 * Update data with given node data and meta with node meta.
 */
@DFExperimental
public fun <T : Any> DataSetBuilder<T>.populateFrom(tree: DataSet<T>): Unit {
    tree.forEach {
        //TODO check if the place is occupied
        data(it.name, it.data)
    }
}

//public fun <T : Any> DataSetBuilder<T>.populateFrom(flow: Flow<NamedData<T>>) {
//    flow.collect {
//        data(it.name, it.data)
//    }
//}

public fun <T : Any> DataSetBuilder<T>.populateFrom(sequence: Sequence<NamedData<T>>) {
    sequence.forEach {
        data(it.name, it.data)
    }
}
