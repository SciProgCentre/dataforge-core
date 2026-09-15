package space.kscience.dataforge.data

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import space.kscience.dataforge.names.Name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds


internal class DataTreeBuilderTest {
    @Test
    fun testTreeBuild() = runTest(timeout = 500.milliseconds) {
        val node = DataTree<Any> {
            node("primary") {
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
            put("a", Data.wrapValue("a"))
            put("b", Data.wrapValue("b"))
        }

        val node = DataTree<Any> {
            node("primary") {
                putValue("a", "a")
                putValue("b", "b")
            }
            putValue("root", "root")
            node("update", updateData)
        }

        assertEquals("a", node["update.a"]?.await())
        assertEquals("a", node["primary.a"]?.await())
    }

    @Test
    fun testMutableDataTree() = runTest(timeout = 500.milliseconds) {
        val subNode = MutableDataTree<Int>()

        val rootNode = MutableDataTree<Int>() {
            launchWriteJobFrom(subNode, backgroundScope, Name.of("sub"))
        }

        repeat(10) {
            subNode.writeValue("value[$it]", it)
        }

        assertEquals(9, subNode.awaitData("value[9]").await())
        assertEquals(8, subNode.awaitData("value[8]").await())
        assertEquals(9, rootNode.awaitData("sub.value[9]").await())
        assertEquals(8, rootNode.awaitData("sub.value[8]").await())
        println("finished")
    }

    @Test
    fun testDynamicTree() = runTest(timeout = 500.milliseconds) {
        val subNode = MutableDataTree<Int>()

        val rootNode = DataTree<Int>{
            observeNode("sub", backgroundScope, subNode)
        }

        //need this for a virtual time dispatcher to complete the subscription before write start
        delay(1)

        repeat(10) {
            subNode.writeValue("value[$it]", it)
        }

        assertEquals(9, subNode.awaitData("value[9]").await())
        assertEquals(8, subNode.awaitData("value[8]").await())
        assertEquals(9, rootNode.awaitData("sub.value[9]").await())
        assertEquals(8, rootNode.awaitData("sub.value[8]").await())
        println("finished")
    }

    @Test
    fun testWriteTreeWithoutPrefix() = runTest {
        val writes = mutableListOf<Name>()
        val sink = DataSink<Int> { name, _ -> writes += name }
        val source = DataTree<Int> {
            putValue("root", 7)
            putValue("nested.value", 42)
        }

        sink.writeAll(source)

        assertEquals(2, writes.size)
        assertEquals(setOf(Name.of("root"), Name.of("nested", "value")), writes.toSet())
    }

    @Test
    fun testWriteTreeWithPrefix() = runTest {
        val writes = mutableListOf<Name>()
        val sink = DataSink<Int> { name, _ -> writes += name }
        val source = DataTree<Int> {
            putValue("root", 7)
            putValue("nested.value", 42)
        }

        sink.writeAll(source, Name.of("prefix"))

        assertEquals(2, writes.size)
        assertEquals(
            setOf(Name.of("prefix", "root"), Name.of("prefix", "nested", "value")),
            writes.toSet(),
        )
    }

    @Test
    fun testWriteEmptyTreeWithoutPrefix() = runTest {
        val writes = mutableListOf<Name>()
        val sink = DataSink<Int> { name, _ -> writes += name }

        sink.writeAll(DataTree<Int>(emptyMap()))

        assertEquals(emptyList(), writes)
    }

    @Test
    fun testLaunchWriteJobFromWithoutPrefix() = runTest {
        val source = MutableDataTree<Int>()
        source.writeValue("nested.value", 42)
        val sink = MutableDataTree<Int>()
        val job = sink.launchWriteJobFrom(source, this)

        assertEquals(42, sink.awaitData("nested.value").await())
        job.cancelAndJoin()
    }
}
