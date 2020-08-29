package hep.dataforge.meta

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

fun ClassSerialDescriptorBuilder.boolean(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
    element(name, Boolean.serializer().descriptor, isOptional = isOptional, annotations = annotations.toList())

fun ClassSerialDescriptorBuilder.string(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
    element(name, String.serializer().descriptor, isOptional = isOptional, annotations = annotations.toList())

fun ClassSerialDescriptorBuilder.int(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
    element(name, Int.serializer().descriptor, isOptional = isOptional, annotations = annotations.toList())

fun ClassSerialDescriptorBuilder.double(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
    element(name, Double.serializer().descriptor, isOptional = isOptional, annotations = annotations.toList())

fun ClassSerialDescriptorBuilder.float(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
    element(name, Float.serializer().descriptor, isOptional = isOptional, annotations = annotations.toList())

fun ClassSerialDescriptorBuilder.long(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
    element(name, Long.serializer().descriptor, isOptional = isOptional, annotations = annotations.toList())

fun ClassSerialDescriptorBuilder.doubleArray(
    name: String,
    isOptional: Boolean = false,
    vararg annotations: Annotation,
) =
    element(name, DoubleArraySerializer().descriptor, isOptional = isOptional, annotations = annotations.toList())

@OptIn(InternalSerializationApi::class)
inline fun <reified E : Enum<E>> ClassSerialDescriptorBuilder.enum(
    name: String,
    isOptional: Boolean = false,
    vararg annotations: Annotation,
) {
    val enumDescriptor = buildSerialDescriptor(serialName, SerialKind.ENUM) {
        enumValues<E>().forEach {
            val fqn = "$serialName.${it.name}"
            val enumMemberDescriptor = buildSerialDescriptor(fqn, StructureKind.OBJECT)
            element(it.name, enumMemberDescriptor)
        }
    }
    element(name, enumDescriptor, isOptional = isOptional, annotations = annotations.toList())
}

@DFExperimental
inline fun <R> Decoder.decodeStructure(
    desc: SerialDescriptor,
    crossinline block: CompositeDecoder.() -> R,
): R {
    val decoder = beginStructure(desc)
    val res = decoder.block()
    decoder.endStructure(desc)
    return res
}

@DFExperimental
inline fun Encoder.encodeStructure(
    desc: SerialDescriptor,
    block: CompositeEncoder.() -> Unit,
) {
    val encoder = beginStructure(desc)
    encoder.block()
    encoder.endStructure(desc)
}

val JSON_PRETTY = Json { prettyPrint = true; useArrayPolymorphism = true }
val JSON_PLAIN = Json { prettyPrint = false; useArrayPolymorphism = true }