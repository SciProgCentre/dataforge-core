package hep.dataforge.meta

import hep.dataforge.names.NameToken
import hep.dataforge.values.ValueSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject

public object MetaItemSerializer : KSerializer<MetaItem> {

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("MetaItem", PolymorphicKind.SEALED) {
        element<Boolean>("isNode")
        element("value", buildSerialDescriptor("MetaItem.value", SerialKind.CONTEXTUAL))
    }

    override fun deserialize(decoder: Decoder): MetaItem {
        decoder.decodeStructure(descriptor) {
            //Force strict serialization order
            require(decodeElementIndex(descriptor) == 0) { "Node flag must be first item serialized" }
            val isNode = decodeBooleanElement(descriptor, 0)
            require(decodeElementIndex(descriptor) == 1) { "Missing MetaItem content" }
            val item = if (isNode) {
                decodeSerializableElement(descriptor,1, MetaSerializer).asMetaItem()
            } else {
                decodeSerializableElement(descriptor,1,ValueSerializer).asMetaItem()
            }
            require(decodeElementIndex(descriptor) == CompositeDecoder.DECODE_DONE){"Serialized MetaItem contains additional fields"}
            return  item
        }
    }

    override fun serialize(encoder: Encoder, value: MetaItem) {
        encoder.encodeStructure(descriptor) {
            encodeBooleanElement(descriptor, 0, value is MetaItemNode)
            when (value) {
                is MetaItemValue -> encodeSerializableElement(descriptor, 1, ValueSerializer, value.value)
                is MetaItemNode -> encodeSerializableElement(descriptor, 1, MetaSerializer, value.node)
            }
        }
    }
}

/**
 * Serialized for meta
 */
public object MetaSerializer : KSerializer<Meta> {

    private val mapSerializer: KSerializer<Map<NameToken, TypedMetaItem<Meta>>> = MapSerializer(
        NameToken,
        MetaItemSerializer//MetaItem.serializer(MetaSerializer)
    )

    override val descriptor: SerialDescriptor  = buildClassSerialDescriptor("Meta")

    override fun deserialize(decoder: Decoder): Meta {
        return if (decoder is JsonDecoder) {
            JsonObject.serializer().deserialize(decoder).toMeta()
        } else {
            object : MetaBase() {
                override val items: Map<NameToken, MetaItem> = mapSerializer.deserialize(decoder)
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Meta) {
        if (encoder is JsonEncoder) {
            JsonObject.serializer().serialize(encoder, value.toJson())
        } else {
            mapSerializer.serialize(encoder, value.items)
        }
    }
}