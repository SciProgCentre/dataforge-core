package space.kscience.dataforge.meta.transformations

import space.kscience.dataforge.meta.*

/**
 * A converter of generic object to and from [Meta]
 */
public interface MetaConverter<T> {
    public fun metaToObject(meta: Meta): T?
    public fun objectToMeta(obj: T): Meta

    public companion object {

        public val meta: MetaConverter<Meta> = object : MetaConverter<Meta> {
            override fun metaToObject(meta: Meta): Meta = meta
            override fun objectToMeta(obj: Meta): Meta = obj
        }

        public val value: MetaConverter<Value> = object : MetaConverter<Value> {
            override fun metaToObject(meta: Meta): Value? = meta.value
            override fun objectToMeta(obj: Value): Meta = Meta(obj)
        }

        public val string: MetaConverter<String> = object : MetaConverter<String> {
            override fun metaToObject(meta: Meta): String? = meta.string
            override fun objectToMeta(obj: String): Meta = Meta(obj.asValue())
        }

        public val boolean: MetaConverter<Boolean> = object : MetaConverter<Boolean> {
            override fun metaToObject(meta: Meta): Boolean? = meta.boolean
            override fun objectToMeta(obj: Boolean): Meta = Meta(obj.asValue())
        }

        public val number: MetaConverter<Number> = object : MetaConverter<Number> {
            override fun metaToObject(meta: Meta): Number? = meta.number
            override fun objectToMeta(obj: Number): Meta = Meta(obj.asValue())
        }

        public val double: MetaConverter<Double> = object : MetaConverter<Double> {
            override fun metaToObject(meta: Meta): Double? = meta.double

            override fun objectToMeta(obj: Double): Meta = Meta(obj.asValue())
        }

        public val float: MetaConverter<Float> = object : MetaConverter<Float> {
            override fun metaToObject(meta: Meta): Float? = meta.float

            override fun objectToMeta(obj: Float): Meta = Meta(obj.asValue())
        }

        public val int: MetaConverter<Int> = object : MetaConverter<Int> {
            override fun metaToObject(meta: Meta): Int? = meta.int

            override fun objectToMeta(obj: Int): Meta = Meta(obj.asValue())
        }

        public val long: MetaConverter<Long> = object : MetaConverter<Long> {
            override fun metaToObject(meta: Meta): Long? = meta.long

            override fun objectToMeta(obj: Long): Meta = Meta(obj.asValue())
        }

        public inline fun <reified E : Enum<E>> enum(): MetaConverter<E> = object : MetaConverter<E> {
            @Suppress("USELESS_CAST")
            override fun metaToObject(meta: Meta): E = meta.enum<E>() as? E ?: error("The Item is not a Enum")

            override fun objectToMeta(obj: E): Meta = Meta(obj.asValue())
        }

        public fun <T> valueList(
            writer: (T) -> Value = { Value.of(it) },
            reader: (Value) -> T
        ): MetaConverter<List<T>> =
            object : MetaConverter<List<T>> {
                override fun metaToObject(meta: Meta): List<T> =
                    meta.value?.list?.map(reader) ?: error("The item is not a value list")

                override fun objectToMeta(obj: List<T>): Meta = Meta(obj.map(writer).asValue())
            }

    }
}

public fun <T : Any> MetaConverter<T>.nullableMetaToObject(item: Meta?): T? = item?.let { metaToObject(it) }
public fun <T : Any> MetaConverter<T>.nullableObjectToMeta(obj: T?): Meta? = obj?.let { objectToMeta(it) }

public fun <T> MetaConverter<T>.valueToObject(value: Value): T? = metaToObject(Meta(value))
