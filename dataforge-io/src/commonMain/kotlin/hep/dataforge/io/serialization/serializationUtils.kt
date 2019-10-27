package hep.dataforge.io.serialization

import kotlinx.serialization.CompositeDecoder
import kotlinx.serialization.Decoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.internal.SerialClassDescImpl

inline class SerialDescriptorBuilder(private val impl: SerialClassDescImpl) {
    fun element(name: String, isOptional: Boolean = false) = impl.addElement(name, isOptional)

    fun annotation(a: Annotation) = impl.pushAnnotation(a)

    fun classAnnotation(a: Annotation) = impl.pushClassAnnotation(a)

    fun descriptor(name: String, block: SerialDescriptorBuilder.() -> Unit) = impl.pushDescriptor(
        SerialDescriptorBuilder(SerialClassDescImpl(name)).apply(block).build()
    )

    fun build(): SerialDescriptor = impl
}

inline fun KSerializer<*>.descriptor(name: String, block: SerialDescriptorBuilder.() -> Unit): SerialDescriptor =
    SerialDescriptorBuilder(SerialClassDescImpl(name)).apply(block).build()

fun Decoder.decodeStructure(
    desc: SerialDescriptor,
    vararg typeParams: KSerializer<*> = emptyArray(),
    block: CompositeDecoder.() -> Unit
) {
    beginStructure(desc, *typeParams).apply(block).endStructure(desc)
}