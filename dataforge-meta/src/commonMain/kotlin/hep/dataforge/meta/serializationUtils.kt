package hep.dataforge.meta

import kotlinx.serialization.*
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

fun SerialDescriptorBuilder.boolean(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
    element(name, Boolean.serializer().descriptor, isOptional = isOptional, annotations = annotations.toList())

fun SerialDescriptorBuilder.string(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
    element(name, String.serializer().descriptor, isOptional = isOptional, annotations = annotations.toList())

fun SerialDescriptorBuilder.int(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
    element(name, Int.serializer().descriptor, isOptional = isOptional, annotations = annotations.toList())

fun SerialDescriptorBuilder.double(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
    element(name, Double.serializer().descriptor, isOptional = isOptional, annotations = annotations.toList())

fun SerialDescriptorBuilder.float(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
    element(name, Float.serializer().descriptor, isOptional = isOptional, annotations = annotations.toList())

fun SerialDescriptorBuilder.long(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
    element(name, Long.serializer().descriptor, isOptional = isOptional, annotations = annotations.toList())

fun SerialDescriptorBuilder.doubleArray(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
    element(name, DoubleArraySerializer().descriptor, isOptional = isOptional, annotations = annotations.toList())

@OptIn(InternalSerializationApi::class)
inline fun <reified E : Enum<E>> SerialDescriptorBuilder.enum(
    name: String,
    isOptional: Boolean = false,
    vararg annotations: Annotation
) {
    val enumDescriptor = SerialDescriptor(serialName, UnionKind.ENUM_KIND) {
        enumValues<E>().forEach {
            val fqn = "$serialName.${it.name}"
            val enumMemberDescriptor = SerialDescriptor(fqn, StructureKind.OBJECT)
            element(it.name, enumMemberDescriptor)
        }
    }
    element(name, enumDescriptor, isOptional = isOptional, annotations = annotations.toList())
}

@DFExperimental
inline fun <R> Decoder.decodeStructure(
    desc: SerialDescriptor,
    vararg typeParams: KSerializer<*> = emptyArray(),
    crossinline block: CompositeDecoder.() -> R
): R {
    val decoder = beginStructure(desc, *typeParams)
    val res = decoder.block()
    decoder.endStructure(desc)
    return res
}

@DFExperimental
inline fun Encoder.encodeStructure(
    desc: SerialDescriptor,
    vararg typeParams: KSerializer<*> = emptyArray(),
    block: CompositeEncoder.() -> Unit
) {
    val encoder = beginStructure(desc, *typeParams)
    encoder.block()
    encoder.endStructure(desc)
}

val PRETTY_JSON = Json(JsonConfiguration(prettyPrint = true, useArrayPolymorphism = true))