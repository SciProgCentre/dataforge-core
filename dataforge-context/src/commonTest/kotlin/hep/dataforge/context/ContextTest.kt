package hep.dataforge.context

import hep.dataforge.names.Name
import hep.dataforge.names.appendLeft
import hep.dataforge.names.toName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class ContextTest {
    class DummyPlugin : AbstractPlugin() {
        override val tag get() = PluginTag("test")

        override fun provideTop(target: String, name: Name): Any? {
            return when (target) {
                "test" -> return name
                else -> super.provideTop(target, name)
            }
        }

        override fun listNames(target: String): Sequence<Name> {
            return when (target) {
                "test" -> sequenceOf("a", "b", "c.d").map { it.toName() }
                else -> super.listNames(target)
            }
        }
    }

    @Test
    fun testPluginManager() {
        Global.plugins.load(DummyPlugin())
        val members = Global.content<Name>("test")
        assertEquals(3, members.count())
        members.forEach {
            assertTrue{it.key == it.value.appendLeft("test")}
        }
    }

}