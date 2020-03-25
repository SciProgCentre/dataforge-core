package hep.dataforge.meta

import kotlin.test.Test


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

}