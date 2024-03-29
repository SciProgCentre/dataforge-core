package space.kscience.dataforge.data

import space.kscience.dataforge.meta.isEmpty
import space.kscience.dataforge.misc.Named
import space.kscience.dataforge.names.Name

public interface NamedData<out T> : Named, Data<T> {
    override val name: Name
    public val data: Data<T>
}

public operator fun NamedData<*>.component1(): Name = name
public operator fun <T: Any> NamedData<T>.component2(): Data<T> = data

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