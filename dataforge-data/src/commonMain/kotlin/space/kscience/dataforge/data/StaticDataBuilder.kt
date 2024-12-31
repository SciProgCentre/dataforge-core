package space.kscience.dataforge.data

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.misc.UnsafeKType
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.parseAsName
import space.kscience.dataforge.names.plus
import kotlin.reflect.KType
import kotlin.reflect.typeOf


public fun interface StaticDataBuilder<T> : DataBuilderScope<T> {
    public fun put(name: Name, data: Data<T>)
}

private class DataMapBuilder<T> : StaticDataBuilder<T> {
    val map = mutableMapOf<Name, Data<T>>()

    override fun put(name: Name, data: Data<T>) {
        if (map.containsKey(name)) {
            error("Duplicate key '$name'")
        } else {
            map.put(name, data)
        }
    }
}

public fun <T> StaticDataBuilder<T>.put(name: String, data: Data<T>) {
    put(name.parseAsName(), data)
}

public inline fun <T, reified T1 : T> StaticDataBuilder<T>.putValue(
    name: String,
    value: T1,
    metaBuilder: MutableMeta.() -> Unit = {}
) {
    put(name, Data(value, Meta(metaBuilder)))
}

public fun <T> StaticDataBuilder<T>.putAll(prefix: Name, block: StaticDataBuilder<T>.() -> Unit) {
    val map = DataMapBuilder<T>().apply(block).map
    map.forEach { (name, data) ->
        put(prefix + name, data)
    }
}

public fun <T> StaticDataBuilder<T>.putAll(prefix: String, block: StaticDataBuilder<T>.() -> Unit) =
    putAll(prefix.parseAsName(), block)

public fun <T> StaticDataBuilder<T>.putAll(prefix: String, tree: DataTree<T>) {
    tree.forEach { data ->
        put(prefix + data.name, data)
    }
}

@UnsafeKType
public fun <T> DataTree.Companion.static(type: KType, block: StaticDataBuilder<T>.() -> Unit): DataTree<T> =
    DataMapBuilder<T>().apply(block).map.asTree(type)

@OptIn(UnsafeKType::class)
public inline fun <reified T> DataTree.Companion.static(noinline block: StaticDataBuilder<T>.() -> Unit): DataTree<T> =
    static(typeOf<T>(), block)