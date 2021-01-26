package hep.dataforge.data

import hep.dataforge.names.toName
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlin.test.Test
import kotlin.test.assertEquals


internal class DataTreeBuilderTest {
    @Test
    fun testDataUpdate() = runBlocking {
        val updateData: DataTree<Any> = DataTree {
            "update" put {
                "a" put Data.static("a")
                "b" put Data.static("b")
            }
        }

        val node = DataTree<Any> {
            emit("primary") {
                data("a", "a")
                data("b", "b")
            }
            data("root", "root")
            populate(updateData)
        }


        assertEquals("a", node.getData("update.a")?.value())
        assertEquals("a", node.getData("primary.a")?.value())
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
                            data("value", it)
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
                    setAndObserve("sub".toName(), subNode)
                }

                launch {
                    rootNode.updatesWithData.collect {
                        println(it)
                    }
                }
                updateJob.join()
                assertEquals(9, rootNode.getData("sub.value")?.value())
                cancel()
            }
        } catch (t: Throwable) {
            if (t !is CancellationException) throw  t
        }

    }
}