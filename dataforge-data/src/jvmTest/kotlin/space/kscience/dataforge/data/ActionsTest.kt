package space.kscience.dataforge.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import space.kscience.dataforge.actions.Action
import space.kscience.dataforge.actions.invoke
import space.kscience.dataforge.actions.mapping
import space.kscience.dataforge.actions.then
import space.kscience.dataforge.misc.DFExperimental
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(DFExperimental::class, ExperimentalCoroutinesApi::class)
internal class ActionsTest {
    @Test
    fun testStaticMapAction() = runTest(timeout = 1.seconds) {
        val plusOne = Action.mapping<Int, Int> {
            result { it + 1 }
        }

        val data: DataTree<Int> = DataTree {
            repeat(10) {
                putValue(it.toString(), it)
            }
        }

        val result = plusOne(data)

        assertEquals(5, result.awaitData("4").await())
    }

    @Test
    fun testDynamicMapAction() = runTest(timeout = 1.seconds) {
        val plusOne = Action.mapping<Int, Int> {
            result { it + 1 }
        }

        val source: MutableDataTree<Int> = MutableDataTree()

        val result: DataTree<Int> = plusOne(source)

        repeat(10) {
            source.writeValue(it.toString(), it)
        }

        assertEquals(5, result.awaitData("4").await())
    }

    object Actions{
        val plusOne = Action.mapping<Int, Int> {
            result { it + 1 }
        }

        val toDouble = Action.mapping<Int, Double> {
            result{it.toDouble()}
        }

        val square = Action.mapping<Double, Double> {
            result{ it*it}
        }
    }

    @Test
    fun actionCompositionTest() = runTest{
        val data: DataTree<Int> = DataTree {
            repeat(10) {
                putValue(it.toString(), it)
            }
        }

        val action = Actions.plusOne.then(Actions.toDouble).then(Actions.square)

        val result = action(data)

        assertEquals(25.0, result.awaitData("4").await())
    }

}