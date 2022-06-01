package space.kscience.dataforge.distributed

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import space.kscience.dataforge.context.Global
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.await
import space.kscience.dataforge.data.get
import space.kscience.dataforge.data.static
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.workspace.Workspace
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RemoteCallTest {

    private lateinit var worker1: NodeWorkspace
    private lateinit var worker2: NodeWorkspace
    private lateinit var workspace: Workspace

    @BeforeAll
    fun before() = runBlocking {
        worker1 = NodeWorkspace(
            context = Global.buildContext("worker1".asName()) {
                plugin(MyPlugin1)
            },
            data = DataTree<Any> {
                static("int", 42)
            },
        )
        worker1.start()

        worker2 = NodeWorkspace(
            context = Global.buildContext("worker2".asName()) {
                plugin(MyPlugin1)
                plugin(MyPlugin2)
            },
        )
        worker2.start()

        workspace = NodeWorkspace(
            context = Global.buildContext {
                plugin(MyPlugin1)
                plugin(MyPlugin2)
                properties {
                    endpoints {
                        Name.of(MyPlugin1.tag.name, "task") on "localhost:${worker1.port}"
                        Name.of(MyPlugin2.tag.name, "task") on "localhost:${worker2.port}"
                    }
                }
            }
        )
    }

    @AfterAll
    fun after() {
        worker1.shutdown()
        worker2.shutdown()
    }

    @Test
    fun `local execution`() = runBlocking {
        assertEquals(42, worker1.data["int".asName()]!!.await())
        val res = worker1
            .produce(Name.of(MyPlugin1.tag.name, "task"), Meta.EMPTY)["result"]!!
            .await()
        assertEquals(43, res)
    }

    @Test
    fun `remote execution`() = runBlocking {
        val remoteRes = workspace
            .produce(Name.of(MyPlugin1.tag.name, "task"), Meta.EMPTY)["result"]!!
            .await()
        assertEquals(43, remoteRes)
    }

    @Test
    fun `transitive execution`() = runBlocking {
        val remoteRes = workspace
            .produce(Name.of(MyPlugin2.tag.name, "task"), Meta.EMPTY)["result"]!!
            .await()
        assertEquals(44, remoteRes)
    }
}
