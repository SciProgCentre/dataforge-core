package space.kscience.dataforge.tables

import space.kscience.dataforge.meta.Meta
import kotlin.reflect.KType

public data class ColumnDef<out T : Any>(
    override val name: String,
    override val type: KType,
    override val meta: Meta
): ColumnHeader<T>