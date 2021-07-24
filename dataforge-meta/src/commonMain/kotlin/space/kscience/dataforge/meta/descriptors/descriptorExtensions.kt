package space.kscience.dataforge.meta.descriptors

import space.kscience.dataforge.names.Name
import space.kscience.dataforge.values.ValueType
import space.kscience.dataforge.values.asValue

public inline fun <reified E : Enum<E>> MetaDescriptorBuilder.enum(
    key: Name,
    default: E?,
    crossinline modifier: MetaDescriptor.() -> Unit = {},
): Unit = value(key) {
    type(ValueType.STRING)
    default?.let {
        default(default)
    }
    allowedValues = enumValues<E>().map { it.asValue() }
    modifier()
}