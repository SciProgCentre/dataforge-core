package hep.dataforge.data

import hep.dataforge.actions.MapAction
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Block the thread and get data content
 */
public fun <T : Any> Data<T>.value(): T = runBlocking { await() }

class ActionsTest {
    val data: DataTree<Int> = runBlocking {
        DataTree {
            repeat(10) {
                data(it.toString(), it)
            }
        }
    }

    @Test
    fun testStaticMapAction() {
        val plusOne = MapAction<Int, Int> {
            result { it + 1 }
        }
        runBlocking {
            val result = plusOne.execute(data)
            assertEquals(2, result.getData("1")?.value())
        }
    }

    @Test
    fun testDynamicMapAction() {
        val plusOne = MapAction<Int, Int> {
            result { it + 1 }
        }
        val datum = runBlocking {
            val result = plusOne.execute(data, scope = this)
            result.getData("1")?.value()
        }
        assertEquals(2, datum)
    }

}