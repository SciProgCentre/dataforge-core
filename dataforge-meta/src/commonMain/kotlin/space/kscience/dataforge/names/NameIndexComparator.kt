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

public fun Meta.getIndexedList(name: Name): List<Meta> = getIndexed(name).entries.sortedWith(
    //sort by index
    compareBy(space.kscience.dataforge.names.NameIndexComparator) { it.key }
).map{it.value}