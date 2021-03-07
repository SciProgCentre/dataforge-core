package space.kscience.dataforge.tables

import space.kscience.dataforge.meta.Scheme
import space.kscience.dataforge.meta.SchemeSpec
import space.kscience.dataforge.meta.enum
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.values.ValueType

public open class ColumnScheme : Scheme() {
    public var title: String? by string()

    public companion object : SchemeSpec<ColumnScheme>(::ColumnScheme)
}

public class ValueColumnScheme : ColumnScheme() {
    public var valueType: ValueType by enum(ValueType.STRING)
}