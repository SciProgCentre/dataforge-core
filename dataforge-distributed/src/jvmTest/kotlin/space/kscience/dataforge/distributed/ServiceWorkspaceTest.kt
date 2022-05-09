package space.kscience.dataforge.distributed

import io.lambdarpc.utils.Endpoint
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.await
import space.kscience.dataforge.data.getData
import space.kscience.dataforge.data.map
import space.kscience.dataforge.data.select
import space.kscience.dataforge.data.static
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.workspace.Workspace
import space.kscience.dataforge.workspace.WorkspacePlugin
import space.kscience.dataforge.workspace.task
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

private class MyPlugin : WorkspacePlugin() {
    override val tag: PluginTag
        get() = Factory.tag

    val task by task<Int> {
        val myInt = workspace.data.select<Int>()
        val res = myInt.getData("int".asName())!!
        emit("result".asName(), res.map { it + 1 })
    }

    companion object Factory : PluginFactory<MyPlugin> {
        override fun invoke(meta: Meta, context: Context): MyPlugin = MyPlugin()

        override val tag: PluginTag
            get() = PluginTag("Plg")

        override val type: KClass<out MyPlugin>
            get() = MyPlugin::class
    }
}

private class RemoteMyPlugin(endpoint: Endpoint) : ClientWorkspacePlugin(endpoint) {
    override val tag: PluginTag
        get() = MyPlugin.tag

    override val tasks: Map<Name, KType>
        get() = mapOf(
            "task".asName() to typeOf<Int>()
        )
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServiceWorkspaceTest {

    private lateinit var worker1: ServiceWorkspace
    private lateinit var workspace: Workspace

    @BeforeAll
    fun before() {
        worker1 = ServiceWorkspace(
            context = Global.buildContext("worker1".asName()) {
                plugin(MyPlugin)
            },
            data = runBlocking {
                DataTree<Any> {
                    static("int", 0)
                }
            },
        )
        worker1.start()

        workspace = Workspace {
            context {
                val endpoint = Endpoint(worker1.address, worker1.port)
                plugin(RemoteMyPlugin(endpoint))
            }
        }
    }

    @AfterAll
    fun after() {
        worker1.shutdown()
    }

    @Test
    fun localExecution() = runBlocking {
        assertEquals(0, worker1.data.getData("int")!!.await())
        val res = worker1
            .produce(Name.of("Plg", "task"), Meta.EMPTY)
            .getData("result".asName())!!
            .await()
        assertEquals(1, res)
    }

    @Test
    fun remoteExecution() = runBlocking {
        val remoteRes = workspace
            .produce(Name.of("Plg", "task"), Meta.EMPTY)
            .getData("result".asName())!!
            .await()
        assertEquals(1, remoteRes)
    }
}
