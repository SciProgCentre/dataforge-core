package space.kscience.dataforge.data

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import space.kscience.dataforge.names.asName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds


internal class DataTreeBuilderTest {
    @Test
    fun testTreeBuild() = runTest(timeout = 500.milliseconds) {
        val node = DataTree.static<Any> {
            node("primary") {
                value("a", "a")
                value("b", "b")
            }
            value("c.d", "c.d")
            value("c.f", "c.f")
        }
        assertEquals("a", node["primary.a"]?.await())
        assertEquals("b", node["primary.b"]?.await())
        assertEquals("c.d", node["c.d"]?.await())
        assertEquals("c.f", node["c.f"]?.await())

    }

    @Test
    fun testDataUpdate() = runTest(timeout = 500.milliseconds) {
        val updateData = DataTree.static<Any> {
            data("a", Data.wrapValue("a"))
            data("b", Data.wrapValue("b"))
        }

        val node = DataTree.static<Any> {
            node("primary") {
                value("a", "a")
                value("b", "b")
            }
            value("root", "root")
            node("update", updateData)
        }

        assertEquals("a", node["update.a"]?.await())
        assertEquals("a", node["primary.a"]?.await())
    }

    @Test
    fun testDynamicUpdates() = runTest(timeout = 500.milliseconds) {
        var job: Job? = null

        val subNode = MutableDataTree<Int>()

        val rootNode = MutableDataTree<Int>() {
            job = launch {
                putAllAndWatch(subNode, "sub".asName())
            }
        }

        repeat(10) {
            subNode.putValue("value[$it]", it)
        }

        assertEquals(9, subNode.awaitData("value[9]").await())
        assertEquals(8, subNode.awaitData("value[8]").await())
        assertEquals(9, rootNode.awaitData("sub.value[9]").await())
        assertEquals(8, rootNode.awaitData("sub.value[8]").await())
        println("finished")
        job?.cancel()
    }
}