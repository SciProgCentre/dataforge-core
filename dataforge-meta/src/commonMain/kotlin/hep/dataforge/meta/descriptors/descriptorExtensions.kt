package hep.dataforge.meta.descriptors

import hep.dataforge.names.Name
import hep.dataforge.values.ValueType
import hep.dataforge.values.asValue

public inline fun <reified E : Enum<E>> NodeDescriptor.enum(
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