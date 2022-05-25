package space.kscience.dataforge.io

import io.ktor.utils.io.core.Input
import io.ktor.utils.io.streams.asInput

public fun IOPlugin.resource(name: String): Binary? = context.javaClass.getResource(name)?.readBytes()?.asBinary()

public inline fun <R> IOPlugin.readResource(name: String, block: Input.() -> R): R =
    context.javaClass.getResource(name)?.openStream()?.asInput()?.block() ?: error("Can't read resource $name")