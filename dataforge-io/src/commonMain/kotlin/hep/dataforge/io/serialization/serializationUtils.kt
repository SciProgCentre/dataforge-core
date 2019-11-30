package hep.dataforge.io.serialization

import hep.dataforge.meta.DFExperimental
import kotlinx.serialization.*
import kotlinx.serialization.internal.*

/**
 * A convenience builder for serial descriptors
 */
inline class SerialDescriptorBuilder(private val impl: SerialClassDescImpl) {
    fun element(
        name: String,
        descriptor: SerialDescriptor?,
        isOptional: Boolean = false,
        vararg annotations: Annotation
    ) {
        impl.addElement(name, isOptional)
        descriptor?.let { impl.pushDescriptor(descriptor) }
        annotations.forEach {
            impl.pushAnnotation(it)
        }
    }

    fun element(
        name: String,
        isOptional: Boolean = false,
        vararg annotations: Annotation,
        block: SerialDescriptorBuilder.() -> Unit
    ) {
        impl.addElement(name, isOptional)
        impl.pushDescriptor(SerialDescriptorBuilder(SerialClassDescImpl(name)).apply(block).build())
        annotations.forEach {
            impl.pushAnnotation(it)
        }
    }

    fun boolean(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
        element(name, BooleanDescriptor, isOptional, *annotations)

    fun string(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
        element(name, StringDescriptor, isOptional, *annotations)

    fun int(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
        element(name, IntDescriptor, isOptional, *annotations)

    fun double(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
        element(name, DoubleDescriptor, isOptional, *annotations)

    fun float(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
        element(name, FloatDescriptor, isOptional, *annotations)

    fun long(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
        element(name, LongDescriptor, isOptional, *annotations)

    fun doubleArray(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
        element(name, DoubleArraySerializer.descriptor, isOptional, *annotations)

    @UseExperimental(InternalSerializationApi::class)
    inline fun <reified E : Enum<E>> enum(name: String, isOptional: Boolean = false, vararg annotations: Annotation) =
        element(name, EnumSerializer(E::class).descriptor, isOptional, *annotations)

    fun classAnnotation(a: Annotation) = impl.pushClassAnnotation(a)

    fun build(): SerialDescriptor = impl
}

inline fun <reified T : Any> KSerializer<T>.descriptor(
    name: String,
    block: SerialDescriptorBuilder.() -> Unit
): SerialDescriptor =
    SerialDescriptorBuilder(SerialClassDescImpl(name)).apply(block).build()

@DFExperimental
inline fun <R> Decoder.decodeStructure(
    desc: SerialDescriptor,
    vararg typeParams: KSerializer<*> = emptyArray(),
    crossinline block:  CompositeDecoder.() -> R
): R {
    val decoder = beginStructure(desc, *typeParams)
    val res = decoder.block()
    decoder.endStructure(desc)
    return res
}

inline fun Encoder.encodeStructure(
    desc: SerialDescriptor,
    vararg typeParams: KSerializer<*> = emptyArray(),
    block: CompositeEncoder.() -> Unit
) {
    val encoder = beginStructure(desc, *typeParams)
    encoder.block()
    encoder.endStructure(desc)
}