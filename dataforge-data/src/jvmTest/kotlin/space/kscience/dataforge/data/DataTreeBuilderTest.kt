package space.kscience.dataforge.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.asName
import kotlin.test.Test
import kotlin.test.assertEquals


internal class DataTreeBuilderTest {
    @Test
    fun testTreeBuild() = runTest {
        val node = DataTree<Any> {
            "primary" put {
                wrap("a", "a")
                wrap("b", "b")
            }
            wrap("c.d", "c.d")
            wrap("c.f", "c.f")
        }
        assertEquals("a", node["primary.a"]?.await())
        assertEquals("b", node["primary.b"]?.await())
        assertEquals("c.d", node["c.d"]?.await())
        assertEquals("c.f", node["c.f"]?.await())

    }

    @OptIn(DFExperimental::class)
    @Test
    fun testDataUpdate() = runTest {
        val updateData = DataTree<Any> {
            "update" put {
                "a" put Data.static("a")
                "b" put Data.static("b")
            }
        }

        val node = DataTree<Any> {
            "primary" put {
                wrap("a", "a")
                wrap("b", "b")
            }
            wrap("root", "root")
            this.putAll(updateData)
        }

        assertEquals("a", node["update.a"]?.await())
        assertEquals("a", node["primary.a"]?.await())
    }

    @Test
    fun testDynamicUpdates() = runBlocking {
        val subNode = MutableDataTree<Int>()

        val rootNode = MutableDataTree<Int> {
            watchBranch("sub".asName(), subNode)
        }

        repeat(10) {
            subNode.wrap("value[$it]", it)
        }

        delay(20)
        assertEquals(9, rootNode["sub.value[9]"]?.await())
    }
}