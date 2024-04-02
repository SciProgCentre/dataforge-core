package space.kscience.dataforge.data

import space.kscience.dataforge.meta.isEmpty
import space.kscience.dataforge.misc.Named
import space.kscience.dataforge.names.Name
import kotlin.reflect.KType

/**
 * An interface implementing a data update event.
 *
 * If [data] is null, then corresponding element should be removed.
 */
public interface DataUpdate<out T> : Named {
    public val type: KType
    override val name: Name
    public val data: Data<T>?
}

public fun <T> DataUpdate(type: KType, name: Name, data: Data<T>?): DataUpdate<T> = object : DataUpdate<T> {
    override val type: KType = type
    override val name: Name = name
    override val data: Data<T>? = data
}

/**
 * A data coupled to a name.
 */
public interface NamedData<out T> : DataUpdate<T>, Data<T> {
    override val data: Data<T>
}

public operator fun NamedData<*>.component1(): Name = name
public operator fun <T> NamedData<T>.component2(): Data<T> = data

private class NamedDataImpl<T>(
    override val name: Name,
    override val data: Data<T>,
) : Data<T> by data, NamedData<T> {
    override fun toString(): String = buildString {
        append("NamedData(name=\"$name\"")
        if (data is StaticData) {
            append(", value=${data.value}")
        }
        if (!data.meta.isEmpty()) {
            append(", meta=${data.meta}")
        }
        append(")")
    }
}

public fun <T> Data<T>.named(name: Name): NamedData<T> = if (this is NamedData) {
    NamedDataImpl(name, this.data)
} else {
    NamedDataImpl(name, this)
}

public fun <T> NamedData(name: Name, data: Data<T>): NamedData<T> = data.named(name)