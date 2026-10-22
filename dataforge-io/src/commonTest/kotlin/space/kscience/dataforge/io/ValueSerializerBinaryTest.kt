package space.kscience.dataforge.io

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import space.kscience.dataforge.meta.*
import kotlin.test.*

@OptIn(ExperimentalSerializationApi::class, ExperimentalStdlibApi::class)
class ValueSerializerBinaryTest {
    @Test
    fun cborBytesStayUnchanged() {
        val nested = ListValue(listOf(False, Null, ListValue.EMPTY, ListValue(listOf(1.asValue(), "n".asValue()))))
        // Golden bytes from the pre-JSON serializer in 4da329b1.
        val cases = listOf(
            True to "f467424f4f4c45414ef5",
            False to "f467424f4f4c45414ef4",
            0.asValue() to "f4664e554d424552fb0000000000000000",
            0.25.asValue() to "f4664e554d424552fb3fd0000000000000",
            "native".asValue() to "f466535452494e47666e6174697665",
            Null to "f4644e554c4c",
            ListValue.EMPTY to "f59fff",
            nested to "f59ff467424f4f4c45414ef4f4644e554c4cf59ffff59ff4664e554d424552fb3ff0000000000000f466535452494e47616effff",
        )
        for ((value, expected) in cases) {
            val bytes = Cbor.encodeToByteArray(ValueSerializer, value)
            assertContentEquals(expected.hexToByteArray(), bytes)
            val restored = Cbor.decodeFromByteArray(ValueSerializer, bytes)
            assertEquals(value.type, restored.type)
            assertEquals(value, restored)
        }
    }
}
