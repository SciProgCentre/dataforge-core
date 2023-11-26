package space.kscience.dataforge.io

import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered


public fun IOPlugin.resource(name: String): Binary? = { }.javaClass.getResource(name)?.readBytes()?.asBinary()

public inline fun <R> IOPlugin.readResource(name: String, block: Source.() -> R): R =
    {  }.javaClass.getResource(name)?.openStream()?.asSource()?.buffered()?.block() ?: error("Can't read resource $name")