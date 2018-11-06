package hep.dataforge.context

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaRepr
import hep.dataforge.meta.buildMeta

/**
 * The tag which contains information about name, group and version of some
 * object. It also could contain any complex rule to define version ranges
 *
 * @author Alexander Nozik
 */
data class PluginTag(
        val name: String,
        val group: String = "",
        val version: String = ""
) : MetaRepr {

    /**
     * Check if given tag is compatible (in range) of this tag
     *
     * @param otherTag
     * @return
     */
    fun matches(otherTag: PluginTag): Boolean {
        return matchesName(otherTag) && matchesGroup(otherTag)
    }

    private fun matchesGroup(otherTag: PluginTag): Boolean {
        return this.group.isEmpty() || this.group == otherTag.group
    }

    private fun matchesName(otherTag: PluginTag): Boolean {
        return this.name == otherTag.name
    }

    override fun toString(): String = listOf(group, name, version).joinToString(separator = ":")

    override fun toMeta(): Meta = buildMeta {
        "name" to name
        "group" to group
        "version" to version
    }

    companion object {

        /**
         * Build new PluginTag from standard string representation
         *
         * @param tag
         * @return
         */
        fun fromString(tag: String): PluginTag {
            val sepIndex = tag.indexOf(":")
            return if (sepIndex >= 0) {
                PluginTag(group = tag.substring(0, sepIndex), name = tag.substring(sepIndex + 1))
            } else {
                PluginTag(tag)
            }
        }
    }
}