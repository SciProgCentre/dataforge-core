@file:Suppress("UNUSED_VARIABLE")

package space.kscience.dataforge.workspace

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Timeout
import space.kscience.dataforge.context.*
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.get
import space.kscience.dataforge.names.plus
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * Make a fake-factory for a one single plugin. Useful for unique or test plugins
 */
public inline fun <reified P : Plugin> P.toFactory(): PluginFactory<P> = object : PluginFactory<P> {
    override fun invoke(meta: Meta, context: Context): P = this@toFactory

    override val tag: PluginTag = this@toFactory.tag
    override val type: KClass<out P> = P::class
}

public fun Workspace.runBlocking(task: String, block: MetaBuilder.() -> Unit = {}): DataSet<Any> = runBlocking {
    produce(task, block)
}


@DFExperimental
class SimpleWorkspaceTest {
    val testPlugin = object : WorkspacePlugin() {
        override val tag: PluginTag = PluginTag("test")

        val test by task<Any> {
            populate(
                workspace.data.map {
                    it.also {
                        logger.info { "Test: $it" }
                    }
                }
            )
        }
    }


    val testPluginFactory = testPlugin.toFactory()

    val workspace = Workspace {

        context {
            plugin(testPluginFactory)
        }

        data {
            repeat(100) {
                static("myData[$it]", it)
            }
        }

        val filterOne by task<Int> {
            workspace.data.selectOne<Int>("myData[12]")?.let { source ->
                emit(source.name, source.map { it })
            }
        }

        val square by task<Int> {
            workspace.data.select<Int>().forEach { data ->
                if (data.meta["testFlag"].boolean == true) {
                    println("flag")
                }
                val value = data.await()
                workspace.logger.info { "Starting square on $value" }
                emit(data.name, data.map { it * it })
            }
        }

        val linear by task<Int> {
            workspace.data.select<Int>().forEach { data ->
                workspace.logger.info { "Starting linear on $data" }
                emit(data.name, data.data.map { it * 2 + 1 })
            }
        }

        val fullSquare by task<Int> {
            val squareData = from(square)
            val linearData = from(linear)
            squareData.forEach { data ->
                val newData: Data<Int> = data.combine(linearData.getData(data.name)!!) { l, r ->
                    l + r
                }
                emit(data.name, newData)
            }
        }

        val sum by task<Int> {
            workspace.logger.info { "Starting sum" }
            val res = from(square).foldToData(0) { l, r ->
                l + r.await()
            }
            emit("sum", res)
        }

        val averageByGroup by task<Int> {
            val evenSum = workspace.data.filter { name, _ ->
                name.toString().toInt() % 2 == 0
            }.select<Int>().foldToData(0) { l, r ->
                l + r.await()
            }

            emit("even", evenSum)
            val oddSum = workspace.data.filter { name, _ ->
                name.toString().toInt() % 2 == 1
            }.select<Int>().foldToData(0) { l, r ->
                l + r.await()
            }
            emit("odd", oddSum)
        }

        val delta by task<Int> {
            val averaged = from(averageByGroup)
            val even = averaged.getData("event")!!
            val odd = averaged.getData("odd")!!
            val res = even.combine(odd) { l, r ->
                l - r
            }
            emit("res", res)
        }

        val customPipe by task<Int> {
            workspace.data.select<Int>().forEach { data ->
                val meta = data.meta.toMutableMeta().apply {
                    "newValue" put 22
                }
                emit(data.name + "new", data.map { (data.meta["value"].int ?: 0) + it })

            }
        }

        target("empty") {}
    }

    @Test
    @Timeout(1)
    fun testWorkspace() {
        runBlocking {
            val node = workspace.runBlocking("sum")
            val res = node.flow().single()
            assertEquals(328350, res.await())
        }
    }

    @Test
    @Timeout(1)
    fun testMetaPropagation() {
        runBlocking {
            val node = workspace.produce("sum") { "testFlag" put true }
            val res = node.flow().single().await()
        }
    }

    @Test
    fun testPluginTask() {
        val tasks = workspace.tasks
        assertTrue { tasks["test.test"] != null }
        //val node = workspace.run("test.test", "empty")
    }

    @Test
    fun testFullSquare() {
        runBlocking {
            val node = workspace.produce("fullSquare")
            println(node.toMeta())
        }
    }

    @Test
    fun testFilter() {
        runBlocking {
            val node = workspace.produce("filterOne")
            assertEquals(12, node.flow().first().await())
        }
    }
}