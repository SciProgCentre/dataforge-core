package hep.dataforge.io.functions

import hep.dataforge.io.IOFormat
import hep.dataforge.meta.MetaRepr

interface FunctionSpec<T : Any, R : Any>: MetaRepr {
    val inputFormat: IOFormat<T>
    val outputFormat: IOFormat<R>
}