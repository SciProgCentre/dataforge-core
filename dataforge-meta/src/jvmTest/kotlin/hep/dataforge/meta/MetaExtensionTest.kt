package hep.dataforge.meta

import org.junit.Test

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
        val meta = buildMeta{"enum" to TestEnum.test.toString()}
        println(meta["enum"].enum<TestEnum>())
    }

}