package hep.dataforge.tables

import hep.dataforge.meta.Meta
import kotlin.reflect.KClass

interface Table {
    fun <T : Any> getValue(row: Int, column: String, type: KClass<out T>): T?
    val columns: Map<String, Column<*>>
    val rows: List<Row>
}

interface Column<out T : Any> {
    val name: String
    val type: KClass<out T>
    val meta: Meta
    val size: Int
    operator fun get(index: Int): T?
}

val Column<*>.indices get() = (0 until size)

operator fun <T : Any> Column<T>.iterator() = iterator {
    for (i in indices){
        yield(get(i))
    }
}

interface Row {
    fun <T : Any> getValue(column: String, type: KClass<out T>): T?
}