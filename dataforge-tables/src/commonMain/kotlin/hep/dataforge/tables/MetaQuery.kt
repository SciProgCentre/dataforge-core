package hep.dataforge.tables

import hep.dataforge.meta.scheme.Scheme
import hep.dataforge.meta.scheme.SchemeSpec
import hep.dataforge.meta.scheme.string

class MetaQuery : Scheme() {
    var field by string()

    companion object : SchemeSpec<MetaQuery>(::MetaQuery)
}