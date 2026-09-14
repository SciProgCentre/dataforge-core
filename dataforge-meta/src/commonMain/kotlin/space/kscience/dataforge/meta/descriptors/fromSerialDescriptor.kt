@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package space.kscience.dataforge.meta.descriptors

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import space.kscience.dataforge.meta.Null
import space.kscience.dataforge.meta.ValueType
import space.kscience.dataforge.meta.asValue

private fun MetaDescriptorBuilder.fromSerialDescriptor(
    descriptor: SerialDescriptor,
    visited: Set<String> = emptySet(),
) {
    if (descriptor.serialName in visited) return
    val newVisited = visited + descriptor.serialName

    when (val kind = descriptor.kind) {
        is PrimitiveKind -> {
            valueTypes = listOf(
                when (kind) {
                    PrimitiveKind.BOOLEAN -> ValueType.BOOLEAN
                    PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT, PrimitiveKind.LONG,
                    PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> ValueType.NUMBER

                    PrimitiveKind.STRING, PrimitiveKind.CHAR -> ValueType.STRING
                }
            )
        }

        SerialKind.ENUM -> {
            valueTypes = listOf(ValueType.STRING)
            allowedValues = (0 until descriptor.elementsCount).map {
                descriptor.getElementName(it).asValue()
            }
        }

        StructureKind.LIST -> {
            val elementDescriptor = descriptor.getElementDescriptor(0)
            if (elementDescriptor.kind is PrimitiveKind || elementDescriptor.kind == SerialKind.ENUM) {
                valueTypes = listOf(ValueType.LIST)
            } else {
                multiple = true
                fromSerialDescriptor(elementDescriptor, newVisited)
            }
        }

        StructureKind.CLASS, StructureKind.OBJECT -> {
            for (i in 0 until descriptor.elementsCount) {
                val elementName = descriptor.getElementName(i)
                val elementDescriptor = descriptor.getElementDescriptor(i)
                node(elementName) {
                    if (!descriptor.isElementOptional(i) && !elementDescriptor.isNullable) {
                        valueRestriction = ValueRestriction.REQUIRED
                    }
                    fromSerialDescriptor(elementDescriptor, newVisited)
                }
            }
        }

        StructureKind.MAP -> {
            val elementDescriptor = descriptor.getElementDescriptor(1)
            childrenDescriptor = MetaDescriptorBuilder().apply {
                fromSerialDescriptor(elementDescriptor, newVisited)
            }
        }

        is PolymorphicKind -> {
            // Polymorphism is not directly supported in MetaDescriptor
        }

        SerialKind.CONTEXTUAL -> {
            // Contextual serialization is not supported
        }
    }

    if (descriptor.isNullable && (descriptor.kind is PrimitiveKind || descriptor.kind == SerialKind.ENUM)) {
        valueTypes = valueTypes?.plus(ValueType.NULL)
        if (descriptor.kind == SerialKind.ENUM) {
            allowedValues += Null
        }
    }
}

/**
 * Build a [MetaDescriptor] from a [SerialDescriptor].
 * Nullable primitive and enum serializers allow an explicit null value.
 */
public fun MetaDescriptor(descriptor: SerialDescriptor): MetaDescriptor = MetaDescriptor {
    fromSerialDescriptor(descriptor)
}


public fun MetaDescriptor(serializer: KSerializer<*>): MetaDescriptor = MetaDescriptor(serializer.descriptor)
