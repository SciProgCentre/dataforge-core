package space.kscience.dataforge.data

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import space.kscience.dataforge.actions.Action
import space.kscience.dataforge.actions.map
import space.kscience.dataforge.misc.DFExperimental
import kotlin.test.assertEquals

@OptIn(DFExperimental::class)
internal class ActionsTest {
    private val data: DataTree<Int> = runBlocking {
        DataTree {
            repeat(10) {
                static(it.toString(), it)
            }
        }
    }

    @Test
    fun testStaticMapAction() {
        val plusOne = Action.map<Int, Int> {
            result { it + 1 }
        }
        runBlocking {
            val result = plusOne.execute(data)
            assertEquals(2, result.getData("1")?.await())
        }
    }

    @Test
    fun testDynamicMapAction() {
        val plusOne = Action.map<Int, Int> {
            result { it + 1 }
        }

        val datum = runBlocking {
            val result = plusOne.execute(data, scope = this)
            result.getData("1")?.await()
        }
        assertEquals(2, datum)
    }

}