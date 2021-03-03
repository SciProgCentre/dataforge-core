package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MetaRepr

/**
 * The tag which contains information about name, group and version of some
 * object. It also could contain any complex rule to define version ranges
 *
 * @author Alexander Nozik
 */
public data class PluginTag(
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
    public fun matches(otherTag: PluginTag): Boolean {
        return matchesName(otherTag) && matchesGroup(otherTag)
    }

    private fun matchesGroup(otherTag: PluginTag): Boolean {
        return this.group.isEmpty() || this.group == otherTag.group
    }

    private fun matchesName(otherTag: PluginTag): Boolean {
        return this.name == otherTag.name
    }

    override fun toString(): String = listOf(group, name, version).joinToString(separator = ":")

    override fun toMeta(): Meta = Meta {
        "name" put name
        "group" put group
        "version" put version
    }

    public companion object {

        public const val DATAFORGE_GROUP: String = "dataforge"

        /**
         * Build new PluginTag from standard string representation
         *
         * @param tag
         * @return
         */
        public fun fromString(tag: String): PluginTag {
            val sepIndex = tag.indexOf(":")
            return if (sepIndex >= 0) {
                PluginTag(group = tag.substring(0, sepIndex), name = tag.substring(sepIndex + 1))
            } else {
                PluginTag(tag)
            }
        }
    }
}