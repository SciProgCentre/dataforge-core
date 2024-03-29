package space.kscience.dataforge.workspace

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import space.kscience.dataforge.data.wrap
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.boolean
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.misc.DFExperimental
import kotlin.test.assertEquals

@OptIn(DFExperimental::class)
internal class CachingWorkspaceTest {

    @Test
    fun testMetaPropagation() = runTest {
        var firstCounter = 0
        var secondCounter = 0

        val workspace = Workspace {
            data {
                //statically initialize data
                repeat(5) {
                    wrap("myData[$it]", it)
                }
            }

            inMemoryCache()

            val doFirst by task<Any> {
                transformEach(allData) { _, name, _ ->
                    firstCounter++
                    println("Done first on $name with flag=${taskMeta["flag"].boolean}")
                }
            }

            val doSecond by task<Any> {
                transformEach(
                    doFirst,
                    dependencyMeta = if (taskMeta["flag"].boolean == true) taskMeta else Meta.EMPTY
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
        //use coroutineScope to wait for the result
        coroutineScope {
            first.launch(this)
            secondA.launch(this)
            secondB.launch(this)
            //repeat to check caching
            secondC.launch(this)
        }

        assertEquals(10, firstCounter)
        assertEquals(10, secondCounter)
    }

}