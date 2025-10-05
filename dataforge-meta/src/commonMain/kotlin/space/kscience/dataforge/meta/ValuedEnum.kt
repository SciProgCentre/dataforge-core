package space.kscience.dataforge.meta

/**
 * A base interface for enums that have an associated raw value of any type `T`.
 * This pattern is common in communication protocols where enums are represented by numbers, strings, etc.
 *
 * It is recommended to implement a `fromValue(value: T)` method in the companion object for discovery.
 *
 * Example:
 * ```
 * enum class MyState(override val value: Int) : ValuedEnum<MyState, Int> {
 *     ON(1), OFF(0);
 *
 *     public companion object {
 *         public fun fromValue(value: Int): MyState? = entries.find { it.value == value }
 *     }
 * }
 * ```
 *
 * @param E The type of the enum itself.
 * @param T The type of the enum's raw value.
 */
public interface ValuedEnum<E : Enum<E>, T> {
    public val value: T
}

/**
 * A wrapper for a [ValuedEnum] that can hold either a known enum [entry]
 * or an unknown raw [value]. This ensures forward compatibility when a remote system
 * sends an enum value that is not yet recognized by the client library.
 *
 * An instance of this class is equal to another instance if and only if both their
 * raw `value` and resolved `entry` are equal. To check for semantic equivalence with
 * an enum constant, use the `entry` property.
 *
 * @param E The type of the enum.
 * @param T The type of the value.
 * @property value The raw value received from the protocol.
 * @property entry The resolved enum entry, or `null` if the raw [value] does not correspond to any known entry.
 */
public data class ValuedEnumValue<E : Enum<E>, T>(
    public val value: T,
    public val entry: E?,
) {

    public companion object {
        /**
         * Creates a [ValuedEnumValue] from a known enum entry.
         * @param entry The known enum constant.
         */
        public fun <E, T> of(entry: E): ValuedEnumValue<E, T> where E : Enum<E>, E : ValuedEnum<E, T> =
            ValuedEnumValue(entry.value, entry)

        /**
         * Creates a [ValuedEnumValue] from a raw value, attempting to resolve it.
         * @param value The raw value.
         * @param entryProvider A function that resolves a value of type `T` to a nullable enum entry.
         */
        public fun <E : Enum<E>, T> fromValue(value: T, entryProvider: (T) -> E?): ValuedEnumValue<E, T> =
            ValuedEnumValue(value, entryProvider(value))
    }
}