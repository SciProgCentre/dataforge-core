package hep.dataforge.tables

import hep.dataforge.meta.Meta
import kotlin.reflect.KClass

public data class ColumnDef<out T : Any>(
    override val name: String,
    override val type: KClass<out T>,
    override val meta: Meta
): ColumnHeader<T>