package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.appendFirst
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame


class ContextTest {
    class TestPlugin(meta: Meta) : AbstractPlugin(meta) {
        override val tag get() = Companion.tag

        companion object : PluginFactory<TestPlugin> {
            override val tag = PluginTag("test")
            override fun build(context: Context, meta: Meta): TestPlugin = TestPlugin(meta)
        }

        override fun content(target: String): Map<Name, Any> {
            return when (target) {
                "test" -> listOf("a", "b", "c.d").associate { Name.parse(it) to Name.parse(it) }
                else -> emptyMap()
            }
        }
    }

    @Test
    fun testPluginManager() {
        val context = Context{
            plugin(TestPlugin())
        }
        val members = context.gather<Name>("test")
        assertEquals(3, members.count())
        members.forEach {
            assertEquals(it.key, it.value.appendFirst("test"))
        }
    }

    @Test
    fun testPluginBuild(){
        val context = Context()
        val plugin = context.request(TestPlugin)
        
        val members = plugin.context.gather<Name>("test")
        assertEquals(3, members.count())
        members.forEach {
            assertEquals(it.key, it.value.appendFirst("test"))
        }
    }


    @Test
    fun testRequestReuse() {
        val context = Context()
        val plugin1 = context.request(TestPlugin)
        val plugin2 = context.request(TestPlugin)

        assertSame(plugin1, plugin2)
        assertSame(plugin1.context, plugin2.context)
    }

    @Test
    fun testRequestDifferentMeta() {
        val context = Context()
        val plugin1 = context.request(TestPlugin, Meta { "a" put 1 })
        val plugin2 = context.request(TestPlugin, Meta { "a" put 2 })

        assertNotSame(plugin1, plugin2)
        assertNotSame(plugin1.context, plugin2.context)
        assertEquals("1", plugin1.meta["a"].string)
        assertEquals("2", plugin2.meta["a"].string)
    }

    @Test
    fun testRequestReuseDifferentMeta() {
        val context = Context()
        val plugin1 = context.request(TestPlugin, Meta { "a" put 1 })
        val plugin2 = context.request(TestPlugin, Meta { "a" put 1 })
        assertSame(plugin1, plugin2)

        val plugin3 = context.request(TestPlugin, Meta { "a" put 2 })
        val plugin4 = context.request(TestPlugin, Meta { "a" put 2 })
        assertSame(plugin3, plugin4)
    }

    @Test
    fun testRequestNoFork() {
        val context = Context()
        context.request(TestPlugin, Meta { "a" put 1 })

        val plugin1 = context.request(TestPlugin, Meta { "a" put 1 })
        assertEquals("1", plugin1.meta["a"].string)

        kotlin.test.assertFails {
            context.request(TestPlugin, Meta { "a" put 2 })
        }
    }


}