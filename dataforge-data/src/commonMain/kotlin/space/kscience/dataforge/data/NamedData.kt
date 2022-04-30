package space.kscience.dataforge.data

import space.kscience.dataforge.meta.isEmpty
import space.kscience.dataforge.misc.Named
import space.kscience.dataforge.names.Name

public interface NamedData<out T : Any> : Named, Data<T> {
    override val name: Name
    public val data: Data<T>
}

private class NamedDataImpl<out T : Any>(
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

public fun <T : Any> Data<T>.named(name: Name): NamedData<T> = if (this is NamedData) {
    NamedDataImpl(name, this.data)
} else {
    NamedDataImpl(name, this)
}

public operator fun <T : Any> NamedData<T>.component1(): Name = name
public operator fun <T : Any> NamedData<T>.component2(): Data<T> = data
