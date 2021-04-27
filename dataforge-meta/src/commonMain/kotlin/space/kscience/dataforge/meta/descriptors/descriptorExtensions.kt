package space.kscience.dataforge.meta.descriptors

import space.kscience.dataforge.names.Name
import space.kscience.dataforge.values.ValueType
import space.kscience.dataforge.values.asValue

public inline fun <reified E : Enum<E>> NodeDescriptorBuilder.enum(
    key: Name,
    default: E?,
    crossinline modifier: ValueDescriptor.() -> Unit = {},
): Unit = value(key) {
    type(ValueType.STRING)
    default?.let {
        default(default)
    }
    allowedValues = enumValues<E>().map { it.asValue() }
    modifier()
}