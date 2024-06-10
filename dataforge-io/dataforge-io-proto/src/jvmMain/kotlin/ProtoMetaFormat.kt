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

    private fun ProtoMeta.ProtoValue.toValue(): Value = when {
        stringValue != null -> stringValue.asValue()
        booleanValue != null -> booleanValue.asValue()
        doubleValue != null -> doubleValue.asValue()
        floatValue != null -> floatValue.asValue()
        int32Value != null -> int32Value.asValue()
        int64Value != null -> int64Value.asValue()
        bytesValue != null -> bytesValue.toByteArray().asValue()
        else -> Null
    }

    override val value: Value?
        get() = when (proto.value_.size) {
            0 -> null
            1 -> proto.value_[0].toValue()
            else -> proto.value_.map { it.toValue() }.asValue()
        }


    override val items: Map<NameToken, Meta>
        get() = proto.items.entries.associate { NameToken.parse(it.key) to ProtoMetaWrapper(it.value) }

    override fun toString(): String = Meta.toString(this)

    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)

    override fun hashCode(): Int = Meta.hashCode(this)
}

internal fun Meta.toProto(): ProtoMeta {


    fun MutableList<ProtoMeta.ProtoValue>.appendProtoValues(value: Value): Unit {
        when (value.type) {
            ValueType.NULL -> {
                //do nothing
            }

            ValueType.NUMBER -> when (value.value) {
                is Int, is Short, is Byte -> add(ProtoMeta.ProtoValue(int32Value = value.int))
                is Long -> add(ProtoMeta.ProtoValue(int64Value = value.long))
                is Float -> add(ProtoMeta.ProtoValue(floatValue = value.float))
                else -> {
                    LoggerFactory.getLogger(ProtoMeta::class.java)
                        .warn("Unknown number type ${value.value} encoded as Double")
                    add(ProtoMeta.ProtoValue(doubleValue = value.double))
                }
            }

            ValueType.STRING -> add(ProtoMeta.ProtoValue(stringValue = value.string))
            ValueType.BOOLEAN -> add(ProtoMeta.ProtoValue(booleanValue = value.boolean))
            ValueType.LIST -> {
                value.list.forEach {
                    if (it.type == ValueType.LIST) {
                        error("Nested lists are not supported")
                    } else {
                        appendProtoValues(it)
                    }
                }
            }
        }
    }

    return ProtoMeta(
        value_ = buildList { value?.let { appendProtoValues(it) } },
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