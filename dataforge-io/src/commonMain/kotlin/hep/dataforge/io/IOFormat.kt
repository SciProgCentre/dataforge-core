package hep.dataforge.io

import kotlinx.io.core.Input
import kotlinx.io.core.Output


interface IOFormat<T : Any> {
    fun write(obj: T, out: Output)
    fun read(input: Input): T
}