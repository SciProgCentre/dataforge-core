package space.kscience.tables

import space.kscience.dataforge.meta.*

public open class ColumnScheme : Scheme() {
    public var title: String? by string()

    public companion object : SchemeSpec<ColumnScheme>(::ColumnScheme)
}

public val ColumnHeader<*>.properties: ColumnScheme get() =  ColumnScheme.read(meta)

public class ValueColumnScheme : ColumnScheme() {
    public var valueType: ValueType by enum(ValueType.STRING)

    public companion object : SchemeSpec<ValueColumnScheme>(::ValueColumnScheme)
}

public val ColumnHeader<Value>.properties: ValueColumnScheme get() =  ValueColumnScheme.read(meta)