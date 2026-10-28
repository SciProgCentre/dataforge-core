package space.kscience.dataforge.names

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.getIndexed


/**
 * A comparator for indices in a [Name]. If both indices are integers, compare them as integers.
 * Null always stays "before" non-null index.
 */
public object NameIndexComparator : Comparator<String?> {
    override fun compare(a: String?, b: String?): Int {
        if (a == b) return 0
        if (a == null) return 1
        if (b == null) return -1
        val aInt = a.toIntOrNull()
        val bInt = b.toIntOrNull()
        return if (aInt != null && bInt != null) {
            aInt.compareTo(bInt)
        } else {
            a.compareTo(b)
        }
    }

}

/**
 * Retrieves a list of `Meta` nodes indexed under the specified `name`. The items are sorted
 * by their index before being returned as a list.
 *
 * @param name The name used to locate and filter indexed `Meta` nodes.
 * @return A list of indexed `Meta` nodes, sorted by their index.
 */
public fun Meta.getIndexedList(name: Name): List<Meta> = getIndexed(name).entries.sortedWith(
    //sort by index
    compareBy(space.kscience.dataforge.names.NameIndexComparator) { it.key }
).map { it.value }

public fun Meta.getIndexedList(name: String): List<Meta> = getIndexedList(name.parseAsName())