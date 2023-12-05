package space.kscience.dataforge.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import space.kscience.dataforge.actions.Action
import space.kscience.dataforge.actions.invoke
import space.kscience.dataforge.actions.mapping
import space.kscience.dataforge.misc.DFExperimental
import kotlin.test.assertEquals

@OptIn(DFExperimental::class, ExperimentalCoroutinesApi::class)
internal class ActionsTest {
    @Test
    fun testStaticMapAction() = runTest {
        val data: DataTree<Int> = DataTree {
            repeat(10) {
                static(it.toString(), it)
            }
        }

        val plusOne = Action.mapping<Int, Int> {
            result { it + 1 }
        }
        val result = plusOne(data)
        assertEquals(2, result["1"]?.await())
    }

    @Test
    fun testDynamicMapAction() = runTest {
        val data: DataSourceBuilder<Int> = DataSource()

        val plusOne = Action.mapping<Int, Int> {
            result { it + 1 }
        }

        val result = plusOne(data)

        repeat(10) {
            data.static(it.toString(), it)
        }

        delay(20)

        assertEquals(2, result["1"]?.await())
        data.close()
    }

}