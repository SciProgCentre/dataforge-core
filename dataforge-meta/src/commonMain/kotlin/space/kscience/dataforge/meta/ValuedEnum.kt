package space.kscience.dataforge.meta

/**
 * A base interface for enums that have an associated raw numeric value.
 * This pattern is common in communication protocols. The numeric type `N` can be any `Number`.
 *
 * It is recommended to implement a `fromValue(value: N)` method in the companion object for discovery.
 *
 * Example:
 * ``` * enum class MyState(override val value: Int) : ValuedEnum<MyState, Int> {
 *     ON(1), OFF(0);
 *
 *     public companion object {
 *         public fun fromValue(value: Int): MyState? = entries.find { it.value == value }
 *     }
 * }
 * ```
 *
 * @param E The type of the enum itself.
 * @param N The numeric type of the enum's value.
 */
public interface ValuedEnum<E : Enum<E>, N : Number> {
    public val value: N
}

/**
 * A wrapper for a [ValuedEnum] that can hold either a known enum [entry]
 * or an unknown raw numeric [value]. This ensures forward compatibility when a remote system
 * sends an enum value that is not yet recognized by the client library.
 *
 * An [ValuedEnumValue] is considered equal to another [ValuedEnumValue] or a [ValuedEnum]
 * if their raw numeric values are equal.
 *
 * @param E The type of the enum.
 * @param N The numeric type of the value.
 * @property value The raw numeric value received from the protocol.
 * @property entry The resolved enum entry, or `null` if the raw [value] does not correspond to any known entry.
 */
public data class ValuedEnumValue<E : Enum<E>, N : Number>(
    public val value: N,
    public val entry: E?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherValue = when (other) {
            is ValuedEnumValue<*, *> -> other.value
            is ValuedEnum<*, *> -> other.value
            else -> return false
        }
        // Use generic Number comparison
        return this.value.toDouble() == otherValue.toDouble() && this.value.toLong() == otherValue.toLong()
    }

    override fun hashCode(): Int = value.hashCode()

    public companion object {
        /**
         * Creates a [ValuedEnumValue] from a known enum entry.
         * @param entry The known enum constant.
         */
        public fun <E, N> of(entry: E): ValuedEnumValue<E, N> where E : Enum<E>, E : ValuedEnum<E, N>, N : Number =
            ValuedEnumValue(entry.value, entry)

        /**
         * Creates a [ValuedEnumValue] from a raw numeric value, attempting to resolve it.
         * @param value The raw numeric value.
         * @param entryProvider A function that resolves a value of type `N` to a nullable enum entry.
         */
        public fun <E : Enum<E>, N : Number> fromValue(value: N, entryProvider: (N) -> E?): ValuedEnumValue<E, N> =
            ValuedEnumValue(value, entryProvider(value))
    }
}