package space.kscience.dataforge.data

import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import space.kscience.dataforge.names.asName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds


internal class DataTreeBuilderTest {
    @Test
    fun testTreeBuild() = runTest(timeout = 500.milliseconds) {
        val node = DataTree<Any> {
            putAll("primary"){
                putValue("a", "a")
                putValue("b", "b")
            }
            putValue("c.d", "c.d")
            putValue("c.f", "c.f")
        }
        assertEquals("a", node["primary.a"]?.await())
        assertEquals("b", node["primary.b"]?.await())
        assertEquals("c.d", node["c.d"]?.await())
        assertEquals("c.f", node["c.f"]?.await())

    }

    @Test
    fun testDataUpdate() = runTest(timeout = 500.milliseconds) {
        val updateData = DataTree<Any> {
            putAll("update") {
                put("a", Data.wrapValue("a"))
                put("b", Data.wrapValue("b"))
            }
        }

        val node = DataTree<Any> {
            putAll("primary") {
                putValue("a", "a")
                putValue("b", "b")
            }
            putValue("root", "root")
            putAll(updateData)
        }

        assertEquals("a", node["update.a"]?.await())
        assertEquals("a", node["primary.a"]?.await())
    }

    @Test
    fun testDynamicUpdates() = runTest(timeout = 500.milliseconds) {
        launch {
            val subNode = MutableDataTree<Int>()

            val rootNode = MutableDataTree<Int>() {
                putAllAndWatch(this@launch, "sub".asName(), subNode)
            }

            repeat(10) {
                subNode.putValue("value[$it]", it)
            }
            subNode.updates.take(10).collect()
            assertEquals(9, rootNode["sub.value[9]"]?.await())
            cancel()
        }.join()
    }
}