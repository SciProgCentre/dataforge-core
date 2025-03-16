package space.kscience.tables

import space.kscience.dataforge.meta.Value

/**
 * A table with random-access mutable cells
 */
public interface MutableTable<T> : Table<T> {
    public operator fun set(row: Int, column: String, value: T?)
}

public operator fun <T, R : T> MutableTable<T>.set(row: Int, column: ColumnHeader<R>, value: R?) {
    set(row, column.name, value)
}

public operator fun <T> MutableTable<T>.set(column: String, values: Iterable<T>) {
    values.forEachIndexed { index, value -> set(index, column, value) }
}

public operator fun MutableTable<Value>.set(row: Int, column: String, value: Any?) {
    set(row, column, Value.of(value))
}