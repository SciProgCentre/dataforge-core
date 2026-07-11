package space.kscience.dataforge.meta

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.parseAsName


/**
 * A converter of generic object to and from [Meta]
 */
public interface MetaConverter<T> : MetaReader<T> {

    /**
     * A descriptor for resulting meta
     */
    override val descriptor: MetaDescriptor? get() = null

    /**
     * Attempt conversion of [source] to an object or return null if conversion failed
     */
    override fun readOrNull(source: Meta): T?

    override fun read(source: Meta): T =
        readOrNull(source) ?: error("Meta $source could not be interpreted by $this")

    public fun convert(obj: T): Meta

    public companion object {

        public val meta: MetaConverter<Meta> = object : MetaConverter<Meta> {
            override fun readOrNull(source: Meta): Meta = source
            override fun convert(obj: Meta): Meta = obj
        }

        public val value: MetaConverter<Value> = object : MetaConverter<Value> {
            override fun readOrNull(source: Meta): Value? = source.value
            override fun convert(obj: Value): Meta = Meta(obj)
        }

        public val string: MetaConverter<String> = object : MetaConverter<String> {
            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.STRING)
            }


            override fun readOrNull(source: Meta): String? = source.string
            override fun convert(obj: String): Meta = Meta(obj.asValue())
        }

        public val name: MetaConverter<Name> = object : MetaConverter<Name> {
            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.STRING)
            }


            override fun readOrNull(source: Meta): Name? = source.string?.parseAsName()
            override fun convert(obj: Name): Meta = Meta(obj.toString())
        }

        public val boolean: MetaConverter<Boolean> = object : MetaConverter<Boolean> {
            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.BOOLEAN)
            }

            override fun readOrNull(source: Meta): Boolean? = source.boolean
            override fun convert(obj: Boolean): Meta = Meta(obj.asValue())
        }

        public val number: MetaConverter<Number> = object : MetaConverter<Number> {
            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.NUMBER)
            }

            override fun readOrNull(source: Meta): Number? = source.number
            override fun convert(obj: Number): Meta = Meta(obj.asValue())
        }

        public val double: MetaConverter<Double> = object : MetaConverter<Double> {
            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.NUMBER)
            }

            override fun readOrNull(source: Meta): Double? = source.double
            override fun convert(obj: Double): Meta = Meta(obj.asValue())
        }

        public val float: MetaConverter<Float> = object : MetaConverter<Float> {
            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.NUMBER)
            }

            override fun readOrNull(source: Meta): Float? = source.float
            override fun convert(obj: Float): Meta = Meta(obj.asValue())
        }

        public val int: MetaConverter<Int> = object : MetaConverter<Int> {
            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.NUMBER)
            }

            override fun readOrNull(source: Meta): Int? = source.int
            override fun convert(obj: Int): Meta = Meta(obj.asValue())
        }

        public val long: MetaConverter<Long> = object : MetaConverter<Long> {
            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.NUMBER)
            }

            override fun readOrNull(source: Meta): Long? = source.long
            override fun convert(obj: Long): Meta = Meta(obj.asValue())
        }

        public inline fun <reified E : Enum<E>> enum(): MetaConverter<E> = object : MetaConverter<E> {
            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.STRING)
                allowedValues(enumValues<E>())
            }

            @Suppress("USELESS_CAST")
            override fun readOrNull(source: Meta): E = source.enum<E>() as? E ?: error("The Item is not a Enum")

            override fun convert(obj: E): Meta = Meta(obj.asValue())
        }

        public val stringList: MetaConverter<List<String>> = object : MetaConverter<List<String>> {
            override fun convert(obj: List<String>): Meta = Meta(obj.map { it.asValue() }.asValue())

            override fun readOrNull(source: Meta): List<String>? = source.stringList
        }

        public fun <T> valueList(
            writer: (T) -> Value = { Value.of(it) },
            reader: (Value) -> T,
        ): MetaConverter<List<T>> = object : MetaConverter<List<T>> {
            override val descriptor: MetaDescriptor = MetaDescriptor {
                valueType(ValueType.LIST)
            }

            override fun readOrNull(source: Meta): List<T>? = source.value?.list?.map(reader)

            override fun convert(obj: List<T>): Meta = Meta(obj.map(writer).asValue())
        }

//        public fun <T> list(
//            elementConverter: MetaConverter<T>
//        ): MetaConverter<List<T>> = object : MetaConverter<List<T>>{
//
//            override val descriptor: MetaDescriptor = MetaDescriptor{
//
//            }
//
//            override fun readOrNull(source: Meta): List<T>? {
//                return source.getIndexedList()
//            }
//
//            override fun convert(obj: List<T>): Meta {
//                TODO("Not yet implemented")
//            }
//        }

        /**
         * Automatically generate [MetaConverter] for a class using its [serializer] and optional [descriptor]
         */
        public fun <T> serializable(
            serializer: KSerializer<T>,
            descriptor: MetaDescriptor? = null,
            jsonEncoder: Json = Json,
        ): MetaConverter<T> = object : MetaConverter<T> {

            override fun readOrNull(source: Meta): T? {
                val json = source.toJson(descriptor)
                return jsonEncoder.decodeFromJsonElement(serializer, json)
            }

            override fun convert(obj: T): Meta {
                val json = jsonEncoder.encodeToJsonElement(serializer, obj)
                return json.toMeta(descriptor)
            }
        }

        /**
         * Automatically generate [MetaConverter] for a class using inferred serializer and optional [descriptor]
         */
        public inline fun <reified T> serializable(
            descriptor: MetaDescriptor? = null,
            jsonEncoder: Json = Json,
        ): MetaConverter<T> = serializable(serializer = serializer(), descriptor, jsonEncoder)

    }
}

public fun <T : Any> MetaConverter<T>.convertNullable(obj: T?): Meta? = obj?.let { convert(it) }


