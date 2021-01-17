package hep.dataforge.workspace

import hep.dataforge.context.*
import hep.dataforge.data.*
import hep.dataforge.meta.*
import hep.dataforge.names.plus
import hep.dataforge.workspace.old.data
import hep.dataforge.workspace.old.dependsOn
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Timeout
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

public fun Workspace.runBlocking(task: String, block: MetaBuilder.() -> Unit = {}): DataSet<Any> = runBlocking{
    execute(task, block)
}


class SimpleWorkspaceTest {
    val testPlugin = object : WorkspacePlugin() {
        override val tag: PluginTag = PluginTag("test")

        val contextTask = task("test", Any::class) {
            map<Any> {
                context.logger.info { "Test: $it" }
            }
        }
    }
    val testPluginFactory = testPlugin.toFactory()

    val workspace = Workspace {

        context {
            plugin(testPluginFactory)
        }

        data {
            repeat(100) {
                data("myData[$it]", it)
            }
        }

        val filterTask = task<Int>("filterOne") {
            model {
                data("myData\\[12\\]")
            }
            map<Int> {
                it
            }
        }

        val square = task<Int>("square") {
            map<Int> { data ->
                if (meta["testFlag"].boolean == true) {
                    println("flag")
                }
                context.logger.info { "Starting square on $data" }
                data * data
            }
        }

        val linear = task<Int>("linear") {
            map<Int> { data ->
                context.logger.info { "Starting linear on $data" }
                data * 2 + 1
            }
        }

        val fullSquare = task<Int>("fullsquare") {
            model {
                val squareDep = dependsOn(square, placement = DataPlacement.into("square"))
                val linearDep = dependsOn(linear, placement = DataPlacement.into("linear"))
            }
            transform<Int> { data ->
                val squareNode = data.branch("square").filterIsInstance<Int>() //squareDep()
                val linearNode = data.branch("linear").filterIsInstance<Int>() //linearDep()
                dataTree {
                    squareNode.flow().collect {
                        val newData: Data<Int> = Data {
                            val squareValue = squareNode.getData(it.name)!!.value()
                            val linearValue = linearNode.getData(it.name)!!.value()
                            squareValue + linearValue
                        }
                        set(name, newData)
                    }
                }
            }
        }

        task<Int>("sum") {
            model {
                dependsOn(square)
            }
            reduce<Int> { data ->
                context.logger.info { "Starting sum" }
                data.values.sum()
            }
        }

        val average = task<Double>("average") {
            reduceByGroup<Int> { env ->
                group("even", filter = { name, _ -> name.toString().toInt() % 2 == 0 }) {
                    result { data ->
                        env.context.logger.info { "Starting even" }
                        data.values.average()
                    }
                }
                group("odd", filter = { name, _ -> name.toString().toInt() % 2 == 1 }) {
                    result { data ->
                        env.context.logger.info { "Starting odd" }
                        data.values.average()
                    }
                }
            }
        }

        task("delta") {
            model {
                dependsOn(average)
            }
            reduce<Double> { data ->
                data["even"]!! - data["odd"]!!
            }
        }

        val customPipeTask = task<Int>("custom") {
            mapAction<Int> {
                meta = meta.toMutableMeta().apply {
                    "newValue" put 22
                }
                name += "new"
                result {
                    meta["value"].int ?: 0 + it
                }
            }
        }

        target("empty") {}
    }

    @Test
    @Timeout(1)
    fun testWorkspace() {
        val node = workspace.runBlocking("sum")
        val res = node.first()
        assertEquals(328350, res?.value())
    }

    @Test
    @Timeout(1)
    fun testMetaPropagation() {
        val node = workspace.runBlocking("sum") { "testFlag" put true }
        val res = node.first()?.value()
    }

    @Test
    fun testPluginTask() {
        val tasks = workspace.stages
        assertTrue { tasks["test.test"] != null }
        //val node = workspace.run("test.test", "empty")
    }

    @Test
    fun testFullSquare() {
        runBlocking {
            val node = workspace.execute("fullsquare")
            println(node.toMeta())
        }
    }

    @Test
    fun testFilter() {
        val node = workspace.runBlocking("filterOne")
        runBlocking {
            assertEquals(12, node.first()?.value())
        }
    }
}