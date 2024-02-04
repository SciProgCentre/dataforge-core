package space.kscience.dataforge.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import space.kscience.dataforge.actions.Action
import space.kscience.dataforge.actions.invoke
import space.kscience.dataforge.actions.mapping
import space.kscience.dataforge.misc.DFExperimental
import kotlin.test.assertEquals

@OptIn(DFExperimental::class)
internal class ActionsTest {
    @Test
    fun testStaticMapAction() = runTest {
        val data: DataTree<Int> = DataTree {
            repeat(10) {
                wrap(it.toString(), it)
            }
        }

        val plusOne = Action.mapping<Int, Int> {
            result { it + 1 }
        }
        val result = plusOne(data)
        assertEquals(2, result["1"]?.await())
    }

    @Test
    fun testDynamicMapAction() = runBlocking {
        val source: MutableDataTree<Int> = MutableDataTree()

        val plusOne = Action.mapping<Int, Int> {
            result { it + 1 }
        }

        val result = plusOne(source)


        repeat(10) {
            source.wrap(it.toString(), it)
        }

        delay(20)

        source.close()
        result.awaitClose()

        assertEquals(2, result["1"]?.await())
    }

}