package space.kscience.dataforge.io.proto

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asInputStream
import kotlinx.io.asOutputStream
import org.slf4j.LoggerFactory
import space.kscience.dataforge.io.MetaFormat
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.names.NameToken

internal class ProtoMetaWrapper(private val proto: ProtoMeta) : Meta {

    private fun ProtoMeta.ProtoValue.toValue(): Value? = when {
        stringValue != null -> stringValue.asValue()
        booleanValue != null -> booleanValue.asValue()
        doubleValue != null -> doubleValue.asValue()
        floatValue != null -> floatValue.asValue()
        int32Value != null -> int32Value.asValue()
        int64Value != null -> int64Value.asValue()
        bytesValue != null -> bytesValue.toByteArray().asValue()
        listValue != null -> listValue.values.mapNotNull { it.toValue() }.asValue()
        float64ListValue != null -> float64ListValue.values.map { it.asValue() }.asValue()
        else -> null
    }

    override val value: Value?
        get() = proto.protoValue?.toValue()


    override val items: Map<NameToken, Meta>
        get() = proto.items.entries.associate { NameToken.parse(it.key) to ProtoMetaWrapper(it.value) }

    override fun toString(): String = Meta.toString(this)

    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)

    override fun hashCode(): Int = Meta.hashCode(this)
}

internal fun Meta.toProto(): ProtoMeta {


    fun Value.toProto(): ProtoMeta.ProtoValue = when (type) {
        ValueType.NULL -> ProtoMeta.ProtoValue()

        ValueType.NUMBER -> when (value) {
            is Int, is Short, is Byte -> ProtoMeta.ProtoValue(int32Value = int)
            is Long -> ProtoMeta.ProtoValue(int64Value = long)
            is Float -> ProtoMeta.ProtoValue(floatValue = float)
            else -> {
                LoggerFactory.getLogger(ProtoMeta::class.java)
                    .warn("Unknown number type ${value} encoded as Double")
                ProtoMeta.ProtoValue(doubleValue = double)
            }
        }

        ValueType.STRING -> ProtoMeta.ProtoValue(stringValue = string)
        ValueType.BOOLEAN -> ProtoMeta.ProtoValue(booleanValue = boolean)
        ValueType.LIST -> ProtoMeta.ProtoValue(listValue = ProtoMeta.ProtoValueList(list.map { it.toProto() }))
    }

    return ProtoMeta(
        protoValue = value?.toProto(),
        items.entries.associate { it.key.toString() to it.value.toProto() }
    )
}


public object ProtoMetaFormat : MetaFormat {
    override fun writeMeta(sink: Sink, meta: Meta, descriptor: MetaDescriptor?) {
        ProtoMeta.ADAPTER.encode(sink.asOutputStream(), meta.toProto())
    }

    override fun readMeta(source: Source, descriptor: MetaDescriptor?): Meta =
        ProtoMetaWrapper(ProtoMeta.ADAPTER.decode(source.asInputStream()))
}