package hep.dataforge.meta

import kotlin.test.Test


class MetaExtensionTest {

    enum class TestEnum{
        test
    }

    @Test
    fun testEnum(){
        val meta = buildMeta{"enum" to TestEnum.test}
        meta["enum"].enum<TestEnum>()
    }
    @Test
    fun testEnumByString(){
        val meta = buildMeta{"enum" to TestEnum.test.name}
        println(meta["enum"].enum<TestEnum>())
    }

}