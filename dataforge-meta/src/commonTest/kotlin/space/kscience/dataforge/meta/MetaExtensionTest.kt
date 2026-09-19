package space.kscience.dataforge.meta

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull


class MetaExtensionTest {

    enum class TestEnum{
        test
    }

    @Test
    fun testEnum(){
        val meta = Meta{"enum" put TestEnum.test}
        meta["enum"].enum<TestEnum>()
    }
    @Test
    fun testEnumByString(){
        val meta = Meta{"enum" put TestEnum.test.name}
        println(meta["enum"].enum<TestEnum>())
    }

    @Test
    fun testEnumWithUnknownValue(){
        val meta = Meta{"enum" put "unknown"}
        assertNull(meta["enum"].enum<TestEnum>())
    }

    @Test
    fun testEnumConverterWithUnknownValue(){
        val meta = Meta("unknown".asValue())
        assertNull(MetaConverter.enum<TestEnum>().readOrNull(meta))
        assertFailsWith<IllegalStateException> {
            MetaConverter.enum<TestEnum>().read(meta)
        }
    }

}