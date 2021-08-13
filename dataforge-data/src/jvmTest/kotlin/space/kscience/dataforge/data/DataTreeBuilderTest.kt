package space.kscience.dataforge.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.asName
import kotlin.test.Test
import kotlin.test.assertEquals


internal class DataTreeBuilderTest {
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
            assertEquals("a", node.getData("primary.a")?.await())
            assertEquals("b", node.getData("primary.b")?.await())
            assertEquals("c.d", node.getData("c.d")?.await())
            assertEquals("c.f", node.getData("c.f")?.await())
        }
    }

    @OptIn(DFExperimental::class)
    @Test
    fun testDataUpdate() = runBlocking {
        val updateData: DataTree<Any> = DataTree {
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
            populate(updateData)
        }

        runBlocking {
            assertEquals("a", node.getData("update.a")?.await())
            assertEquals("a", node.getData("primary.a")?.await())
        }
    }

    @Test
    fun testDynamicUpdates() = runBlocking {
        try {
            lateinit var updateJob: Job
            supervisorScope {
                val subNode = ActiveDataTree<Int> {
                    updateJob = launch {
                        repeat(10) {
                            delay(10)
                            static("value", it)
                        }
                        delay(10)
                    }
                }
                launch {
                    subNode.updatesWithData.collect {
                        println(it)
                    }
                }
                val rootNode = ActiveDataTree<Int> {
                    setAndObserve("sub".asName(), subNode)
                }

                launch {
                    rootNode.updatesWithData.collect {
                        println(it)
                    }
                }
                updateJob.join()
                assertEquals(9, rootNode.getData("sub.value")?.await())
                cancel()
            }
        } catch (t: Throwable) {
            if (t !is CancellationException) throw  t
        }

    }
}