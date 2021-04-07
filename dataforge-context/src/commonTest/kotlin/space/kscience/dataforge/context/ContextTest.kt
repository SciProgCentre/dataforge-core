package space.kscience.dataforge.context

import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.appendLeft
import space.kscience.dataforge.names.toName
import kotlin.test.Test
import kotlin.test.assertEquals


class ContextTest {
    class DummyPlugin : AbstractPlugin() {
        override val tag get() = PluginTag("test")

        override fun content(target: String): Map<Name, Any> {
            return when(target){
                "test" -> listOf("a", "b", "c.d").associate { it.toName() to it.toName() }
                else -> emptyMap()
            }
        }

    }

    @Test
    fun testPluginManager() {
        val context = Global.buildContext{
            name("test")
            plugin(DummyPlugin())
        }
        val members = context.gather<Name>("test")
        assertEquals(3, members.count())
        members.forEach {
            assertEquals(it.key, it.value.appendLeft("test"))
        }
    }

}