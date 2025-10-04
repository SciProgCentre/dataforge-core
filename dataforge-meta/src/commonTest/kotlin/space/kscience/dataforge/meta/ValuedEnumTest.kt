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

    class DeviceConfiguration : Scheme(MutableMeta()) {
        var status by intValuedEnum(DeviceStatus::fromValue, DeviceStatus.OFFLINE)
        var optionalStatus by intValuedEnum(DeviceStatus::fromValue)

        companion object : SchemeSpec<DeviceConfiguration>(::DeviceConfiguration)
    }

    @Test
    fun testDefaultValue() {
        val config = DeviceConfiguration()
        assertEquals(DeviceStatus.OFFLINE, config.status.entry)
        assertEquals(0, config.status.value)
    }

    @Test
    fun testWriteAndReadKnownValue() {
        val meta = MutableMeta()
        val config = DeviceConfiguration()
        config.retarget(meta)

        // Write a known enum entry
        config.status = ValuedEnumValue.of(DeviceStatus.ONLINE)

        // Check the property value
        assertEquals(DeviceStatus.ONLINE, config.status.entry)
        assertEquals(1, config.status.value)

        // Check the underlying Meta value
        assertEquals(1, meta["status"].int)
    }

    @Test
    fun testReadRawValue() {
        val meta = MutableMeta()
        meta["status"] = 2 // Set a raw integer value
        val config = DeviceConfiguration()
        config.retarget(meta)

        assertEquals(DeviceStatus.ERROR, config.status.entry)
        assertEquals(2, config.status.value)
    }

    @Test
    fun testReadUnknownValue() {
        val meta = MutableMeta()
        meta["status"] = 99 // Set an unknown raw value
        val config = DeviceConfiguration()
        config.retarget(meta)

        // The entry should be null, but the raw value must be preserved
        assertNull(config.status.entry)
        assertEquals(99, config.status.value)
    }

    @Test
    fun testWriteUnknownAndRead() {
        val meta = MutableMeta()
        val config = DeviceConfiguration()
        config.retarget(meta)

        // Write an unknown value
        config.status = ValuedEnumValue.fromValue(99, DeviceStatus::fromValue)

        // Check the property value
        assertNull(config.status.entry)
        assertEquals(99, config.status.value)

        // Check the underlying Meta value
        assertEquals(99, meta["status"].int)

        // Check that a new config reading this meta gets the same result
        val newConfig = DeviceConfiguration()
        newConfig.retarget(meta)
        assertNull(newConfig.status.entry)
        assertEquals(99, newConfig.status.value)
    }

    @Test
    fun testEquality() {
        val onlineByEnum = ValuedEnumValue.of(DeviceStatus.ONLINE)
        val onlineByValue = ValuedEnumValue.fromValue(1, DeviceStatus::fromValue)
        // Simulate unknown value (e.g., from an older version of the enum)
        val onlineUnknown = ValuedEnumValue.fromValue<DeviceStatus, Int>(1) { null }

        assertEquals(onlineByEnum, onlineByValue)
        assertEquals(onlineByEnum, ValuedEnumValue.of(DeviceStatus.ONLINE))
        assertEquals(ValuedEnumValue.of(DeviceStatus.ONLINE), onlineByEnum)
        assertEquals(onlineByValue, onlineUnknown) // Equality by raw value is key
        assertNotEquals(onlineByEnum, ValuedEnumValue.of(DeviceStatus.OFFLINE))
    }

    @Test
    fun testNullableProperty() {
        val meta = MutableMeta()
        val config = DeviceConfiguration()
        config.retarget(meta)

        // Initially null
        assertNull(config.optionalStatus)
        assertNull(meta["optionalStatus"])

        // Set to a value
        config.optionalStatus = ValuedEnumValue.of(DeviceStatus.ERROR)
        assertEquals(DeviceStatus.ERROR, config.optionalStatus?.entry)
        assertEquals(2, meta["optionalStatus"].int)

        // Set back to null
        config.optionalStatus = null
        assertNull(config.optionalStatus)
        assertNull(meta["optionalStatus"])
    }
}