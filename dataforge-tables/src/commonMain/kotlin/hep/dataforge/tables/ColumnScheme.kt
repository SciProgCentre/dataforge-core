package hep.dataforge.tables

import hep.dataforge.meta.Scheme
import hep.dataforge.meta.SchemeSpec
import hep.dataforge.meta.enum
import hep.dataforge.meta.string
import hep.dataforge.values.ValueType

public open class ColumnScheme : Scheme() {
    public var title: String? by string()

    public companion object : SchemeSpec<ColumnScheme>(::ColumnScheme)
}

public class ValueColumnScheme : ColumnScheme() {
    public var valueType: ValueType by enum(ValueType.STRING)
}