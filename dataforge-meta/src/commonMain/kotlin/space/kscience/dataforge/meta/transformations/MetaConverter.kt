package space.kscience.dataforge.meta.transformations

import space.kscience.dataforge.meta.*
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A converter of generic object to and from [Meta]
 */
public interface MetaConverter<T> {

    /**
     * Runtime type of [T]
     */
    public val type: KType

    /**
     * A descriptor for resulting meta
     */
    public val descriptor: MetaDescriptor get() = MetaDescriptor.EMPTY

    /**
     * Attempt conversion of [meta] to an object or return null if conversion failed
     */
    public fun metaToObjectOrNull(meta: Meta): T?

    public fun metaToObject(meta: Meta): T =
        metaToObjectOrNull(meta) ?: error("Meta $meta could not be interpreted by $this")

    public fun objectToMeta(obj: T): Meta

    public companion object {

        public val meta: MetaConverter<Meta> = object : MetaConverter<Meta> {
            override val type: KType = typeOf<Meta>()

            override fun metaToObjectOrNull(meta: Meta): Meta = meta
            override fun objectToMeta(obj: Meta): Meta = obj
        }

        public val value: MetaConverter<Value> = object : MetaConverter<Value> {
            override val type: KType = typeOf<Value>()

            override fun metaToObjectOrNull(meta: Meta): Value? = meta.value
            override fun objectToMeta(obj: Value): Meta = Meta(obj)
        }

        public val string: MetaConverter<String> = object : MetaConverter<String> {
            override val type: KType = typeOf<String>()

            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.STRING)
            }


            override fun metaToObjectOrNull(meta: Meta): String? = meta.string
            override fun objectToMeta(obj: String): Meta = Meta(obj.asValue())
        }

        public val boolean: MetaConverter<Boolean> = object : MetaConverter<Boolean> {
            override val type: KType = typeOf<Boolean>()

            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.BOOLEAN)
            }

            override fun metaToObjectOrNull(meta: Meta): Boolean? = meta.boolean
            override fun objectToMeta(obj: Boolean): Meta = Meta(obj.asValue())
        }

        public val number: MetaConverter<Number> = object : MetaConverter<Number> {
            override val type: KType = typeOf<Number>()

            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.NUMBER)
            }

            override fun metaToObjectOrNull(meta: Meta): Number? = meta.number
            override fun objectToMeta(obj: Number): Meta = Meta(obj.asValue())
        }

        public val double: MetaConverter<Double> = object : MetaConverter<Double> {
            override val type: KType = typeOf<Double>()

            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.NUMBER)
            }

            override fun metaToObjectOrNull(meta: Meta): Double? = meta.double
            override fun objectToMeta(obj: Double): Meta = Meta(obj.asValue())
        }

        public val float: MetaConverter<Float> = object : MetaConverter<Float> {
            override val type: KType = typeOf<Float>()

            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.NUMBER)
            }

            override fun metaToObjectOrNull(meta: Meta): Float? = meta.float
            override fun objectToMeta(obj: Float): Meta = Meta(obj.asValue())
        }

        public val int: MetaConverter<Int> = object : MetaConverter<Int> {
            override val type: KType = typeOf<Int>()

            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.NUMBER)
            }

            override fun metaToObjectOrNull(meta: Meta): Int? = meta.int
            override fun objectToMeta(obj: Int): Meta = Meta(obj.asValue())
        }

        public val long: MetaConverter<Long> = object : MetaConverter<Long> {
            override val type: KType = typeOf<Long>()

            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.NUMBER)
            }

            override fun metaToObjectOrNull(meta: Meta): Long? = meta.long
            override fun objectToMeta(obj: Long): Meta = Meta(obj.asValue())
        }

        public inline fun <reified E : Enum<E>> enum(): MetaConverter<E> = object : MetaConverter<E> {
            override val type: KType = typeOf<E>()

            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.STRING)
                allowedValues(enumValues<E>())
            }

            @Suppress("USELESS_CAST")
            override fun metaToObjectOrNull(meta: Meta): E = meta.enum<E>() as? E ?: error("The Item is not a Enum")

            override fun objectToMeta(obj: E): Meta = Meta(obj.asValue())
        }

        public fun <T> valueList(
            writer: (T) -> Value = { Value.of(it) },
            reader: (Value) -> T,
        ): MetaConverter<List<T>> =
            object : MetaConverter<List<T>> {
                override val type: KType = typeOf<List<T>>()

                override val descriptor: MetaDescriptor = MetaDescriptor {
                    valueType(ValueType.LIST)
                }

                override fun metaToObjectOrNull(meta: Meta): List<T>? = meta.value?.list?.map(reader)

                override fun objectToMeta(obj: List<T>): Meta = Meta(obj.map(writer).asValue())
            }

    }
}

public fun <T : Any> MetaConverter<T>.nullableMetaToObject(item: Meta?): T? = item?.let { metaToObject(it) }
public fun <T : Any> MetaConverter<T>.nullableObjectToMeta(obj: T?): Meta? = obj?.let { objectToMeta(it) }

public fun <T> MetaConverter<T>.valueToObject(value: Value): T? = metaToObject(Meta(value))
