package space.kscience.dataforge.data

import kotlinx.coroutines.test.runTest
import space.kscience.dataforge.actions.Action
import space.kscience.dataforge.actions.invoke
import space.kscience.dataforge.actions.mapping
import space.kscience.dataforge.misc.DFExperimental
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

@OptIn(DFExperimental::class)
internal class ActionsTest {
    @Test
    fun testStaticMapAction() = runTest(timeout = 500.milliseconds) {
        val plusOne = Action.mapping<Int, Int> {
            result { it + 1 }
        }

        val data: DataTree<Int> = DataTree {
            repeat(10) {
                putValue(it.toString(), it)
            }
        }

        val result = plusOne(data)
        assertEquals(2, result["1"]?.await())
    }

    @Test
    fun testDynamicMapAction() = runTest(timeout = 500.milliseconds) {
        val plusOne = Action.mapping<Int, Int> {
            result { it + 1 }
        }

        val source: MutableDataTree<Int> = MutableDataTree()

        val result = plusOne(source)


        repeat(10) {
            source.updateValue(it.toString(), it)
        }

//        result.updates.take(10).onEach { println(it.name) }.collect()

        assertEquals(2, result["1"]?.await())
    }

}