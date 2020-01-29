package hep.dataforge.tables

import hep.dataforge.meta.scheme.Scheme
import hep.dataforge.meta.scheme.SchemeSpec

class ColumnScheme : Scheme() {
    companion object : SchemeSpec<ColumnScheme>(::ColumnScheme)
}