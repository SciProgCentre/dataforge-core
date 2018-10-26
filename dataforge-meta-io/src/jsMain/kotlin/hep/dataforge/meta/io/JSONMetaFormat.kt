package hep.dataforge.meta.io

import hep.dataforge.meta.Meta
import kotlinx.io.core.Input
import kotlinx.io.core.Output
import kotlinx.io.core.readText
import kotlinx.io.core.writeText
import kotlin.js.Json

internal actual fun writeJson(meta: Meta, out: Output) {
    out.writeText(JSON.stringify(meta))
}

internal actual fun readJson(input: Input, length: Int): Meta {
    val json: Json = JSON.parse(input.readText(max = if (length > 0) length else Int.MAX_VALUE))
    return JSMeta(json)
}