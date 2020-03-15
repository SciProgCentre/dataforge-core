package hep.dataforge.tables

import hep.dataforge.meta.enum
import hep.dataforge.meta.scheme.Scheme
import hep.dataforge.meta.scheme.SchemeSpec
import hep.dataforge.meta.scheme.enum
import hep.dataforge.meta.scheme.string
import hep.dataforge.values.ValueType

open class ColumnScheme : Scheme() {
    var title by string()

    companion object : SchemeSpec<ColumnScheme>(::ColumnScheme)
}

class ValueColumnScheme : ColumnScheme() {
    var valueType by enum(ValueType.STRING){enum<ValueType>()}
}