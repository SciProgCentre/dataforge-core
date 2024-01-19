package space.kscience.dataforge.data

import kotlinx.coroutines.*
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.asName
import kotlin.test.Test
import kotlin.test.assertEquals


internal class LegacyGenericDataTreeBuilderTest {
    @Test
    fun testTreeBuild() = runBlocking {
        val node = DataTree<Any> {
            "primary" put {
                static("a", "a")
                static("b", "b")
            }
            static("c.d", "c.d")
            static("c.f", "c.f")
        }
        runBlocking {
            assertEquals("a", node["primary.a"]?.await())
            assertEquals("b", node["primary.b"]?.await())
            assertEquals("c.d", node["c.d"]?.await())
            assertEquals("c.f", node["c.f"]?.await())
        }
    }

    @OptIn(DFExperimental::class)
    @Test
    fun testDataUpdate() = runBlocking {
        val updateData = DataTree<Any> {
            "update" put {
                "a" put Data.static("a")
                "b" put Data.static("b")
            }
        }

        val node = DataTree<Any> {
            "primary" put {
                static("a", "a")
                static("b", "b")
            }
            static("root", "root")
            populateFrom(updateData)
        }

        runBlocking {
            assertEquals("a", node["update.a"]?.await())
            assertEquals("a", node["primary.a"]?.await())
        }
    }

    @Test
    fun testDynamicUpdates() = runBlocking {
        try {
            lateinit var updateJob: Job
            supervisorScope {
                val subNode = ObservableDataTree<Int>(this) {
                    updateJob = launch {
                        repeat(10) {
                            delay(10)
                            static("value", it)
                        }
                        delay(10)
                    }
                }
                launch {
                    subNode.updates().collect {
                        println(it)
                    }
                }
                val rootNode = ObservableDataTree<Int>(this) {
                    setAndWatch("sub".asName(), subNode)
                }

                launch {
                    rootNode.updates().collect {
                        println(it)
                    }
                }
                updateJob.join()
                assertEquals(9, rootNode["sub.value"]?.await())
                cancel()
            }
        } catch (t: Throwable) {
            if (t !is CancellationException) throw t
        }

    }
}