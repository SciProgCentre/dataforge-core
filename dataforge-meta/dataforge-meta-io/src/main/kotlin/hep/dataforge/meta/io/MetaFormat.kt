package hep.dataforge.meta.io

import hep.dataforge.meta.Meta
import kotlinx.io.core.Input
import kotlinx.io.core.Output

/**
 * A format for meta serialization
 */
interface MetaFormat {
    val name : String
    val key : Short

    suspend fun write(meta: Meta, out: Output)
    suspend fun read(input: Input): Meta
}

/**
 * Resolve format by its name. Null if not provided
 */
expect fun resolveFormat(name: String): MetaFormat?

/**
 * Resolve format by its binary key. Null if not provided
 */
expect fun resolveFormat(key: Short): MetaFormat?