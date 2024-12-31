package space.kscience.dataforge.data

import space.kscience.dataforge.meta.isEmpty
import space.kscience.dataforge.misc.Named
import space.kscience.dataforge.names.Name

/**
 * A data coupled to a name.
 */
public interface NamedData<out T> : Data<T>, Named


private class NamedDataImpl<T>(
    override val name: Name,
    val data: Data<T>,
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
    NamedDataImpl(name, this)
} else {
    NamedDataImpl(name, this)
}

public fun <T> NamedData(name: Name, data: Data<T>): NamedData<T> = data.named(name)