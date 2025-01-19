package space.kscience.dataforge.meta

import space.kscience.dataforge.meta.descriptors.Described

public interface MetaReader<out T> : Described {

    /**
     * Read the source meta into an object and return null if Meta could not be interpreted as a target type
     */
    public fun readOrNull(source: Meta): T?

    /**
     * Read generic read-only meta with this [MetaReader] producing instance of the desired type.
     * Throws an error if conversion could not be done.
     */
    public fun read(source: Meta): T = readOrNull(source) ?: error("Meta $source could not be interpreted by $this")
}


public fun <T : Any> MetaReader<T>.readNullable(item: Meta?): T? = item?.let { read(it) }
public fun <T> MetaReader<T>.readValue(value: Value): T? = read(Meta(value))
