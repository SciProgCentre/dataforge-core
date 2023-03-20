package space.kscience.dataforge.workspace

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import space.kscience.dataforge.data.startAll
import space.kscience.dataforge.data.static
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.boolean
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.misc.DFExperimental
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class, DFExperimental::class)
internal class CachingWorkspaceTest {

    @Test
    fun testMetaPropagation() = runTest {
        var firstCounter = 0
        var secondCounter = 0

        val workspace = Workspace {
            data {
                //statically initialize data
                repeat(5) {
                    static("myData[$it]", it)
                }
            }

            inMemoryCache()

            val doFirst by task<Any> {
                pipeFrom(allData) { _, name, _ ->
                    firstCounter++
                    println("Done first on $name with flag=${taskMeta["flag"].boolean}")
                }
            }

            @Suppress("UNUSED_VARIABLE")
            val doSecond by task<Any> {
                pipeFrom(
                    doFirst,
                    dependencyMeta = if(taskMeta["flag"].boolean == true) taskMeta else Meta.EMPTY
                ) { _, name, _ ->
                    secondCounter++
                    println("Done second on $name with flag=${taskMeta["flag"].boolean ?: false}")
                }
            }
        }

        val first = workspace.produce("doFirst")
        val secondA = workspace.produce("doSecond")
        val secondB = workspace.produce("doSecond", Meta { "flag" put true })
        val secondC = workspace.produce("doSecond")
        coroutineScope {
            first.startAll(this)
            secondA.startAll(this)
            secondB.startAll(this)
            //repeat to check caching
            secondC.startAll(this)
        }
        assertEquals(10, firstCounter)
        assertEquals(10, secondCounter)
    }

}