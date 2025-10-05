package space.kscience.dataforge.meta

import space.kscience.dataforge.misc.DFExperimental
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

@OptIn(DFExperimental::class)
class ValuedEnumTest {

    enum class DeviceStatus(override val value: Int) : ValuedEnum<DeviceStatus, Int> {
        OFFLINE(0),
        ONLINE(1),
        ERROR(2);

        public companion object {
            public fun fromValue(value: Int): DeviceStatus? = entries.find { it.value == value }
        }
    }

    enum class LogLevel(override val value: String) : ValuedEnum<LogLevel, String> {
        DEBUG("D"),
        INFO("I"),
        WARNING("W");

        public companion object {
            public fun fromValue(value: String): LogLevel? = entries.find { it.value == value }
        }
    }

    class DeviceConfiguration : Scheme() {
        var status by intValuedEnum(DeviceStatus::fromValue, DeviceStatus.OFFLINE)
        var logLevel by valuedEnum(Meta::string, String::asValue, LogLevel::fromValue, LogLevel.INFO)
        var optionalStatus by intValuedEnum(DeviceStatus::fromValue)

        companion object : SchemeSpec<DeviceConfiguration>(::DeviceConfiguration)
    }

    @Test
    fun testDefaultValue() {
        val config = DeviceConfiguration()
        assertEquals(DeviceStatus.OFFLINE, config.status.entry)
        assertEquals(0, config.status.value)
        assertEquals(LogLevel.INFO, config.logLevel.entry)
    }

    @Test
    fun testWriteAndReadKnownValue() {
        val config = DeviceConfiguration()
        config.status = ValuedEnumValue.of(DeviceStatus.ONLINE)

        assertEquals(DeviceStatus.ONLINE, config.status.entry)
        assertEquals(1, config.status.value)
        assertEquals(1, config.meta["status"].int)
    }

    @Test
    fun testReadUnknownIntValue() {
        val meta = MutableMeta { "status" put 99 }
        val config = DeviceConfiguration()
        config.retarget(meta)

        assertNull(config.status.entry)
        assertEquals(99, config.status.value)
    }

    @Test
    fun testStringValuedEnum() {
        val config = DeviceConfiguration()
        config.logLevel = ValuedEnumValue.of(LogLevel.WARNING)

        assertEquals(LogLevel.WARNING, config.logLevel.entry)
        assertEquals("W", config.logLevel.value)
        assertEquals("W", config.meta["logLevel"].string)
    }

    @Test
    fun testReadUnknownStringValue() {
        val meta = MutableMeta { "logLevel" put "FATAL" }
        val config = DeviceConfiguration()
        config.retarget(meta)

        assertNull(config.logLevel.entry)
        assertEquals("FATAL", config.logLevel.value)
    }

    @Test
    fun testEqualityAndEquivalence() {
        val onlineByEnum = ValuedEnumValue.of(DeviceStatus.ONLINE) // { value: 1, entry: ONLINE }
        val onlineByValue = ValuedEnumValue.fromValue(1, DeviceStatus::fromValue) // { value: 1, entry: ONLINE }
        val offlineByEnum = ValuedEnumValue.of(DeviceStatus.OFFLINE) // { value: 0, entry: OFFLINE }
        val unknownByValue = ValuedEnumValue.fromValue(99, DeviceStatus::fromValue) // { value: 99, entry: null }

        assertEquals(onlineByEnum, onlineByValue, "Wrappers for the same resolved enum should be equal")
        assertNotEquals(onlineByEnum, offlineByEnum, "Wrappers for different enums should not be equal")
        assertNotEquals(onlineByEnum, unknownByValue, "A resolved wrapper and an unresolved one should not be equal")

        assertEquals(DeviceStatus.ONLINE, onlineByValue.entry, "The resolved entry should match the expected enum")
        assertEquals(DeviceStatus.OFFLINE, offlineByEnum.entry, "The resolved entry should match the expected enum")
        assertNull(unknownByValue.entry, "The entry for an unknown value must be null")

        assertEquals(1, onlineByValue.value)
        assertEquals(99, unknownByValue.value)
    }
}