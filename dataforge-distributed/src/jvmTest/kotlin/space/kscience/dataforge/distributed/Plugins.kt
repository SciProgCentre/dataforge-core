package space.kscience.dataforge.distributed

import kotlinx.serialization.serializer
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.context.info
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.data.data
import space.kscience.dataforge.data.getByType
import space.kscience.dataforge.data.map
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.workspace.WorkspacePlugin
import space.kscience.dataforge.workspace.fromTask
import space.kscience.dataforge.workspace.task
import kotlin.reflect.KClass

internal class MyPlugin1 : WorkspacePlugin() {
    override val tag: PluginTag
        get() = Factory.tag

    val task by task<Int>(serializer()) {
        workspace.logger.info { "In ${tag.name}.task" }
        val myInt = workspace.data.getByType<Int>("int")!!
        data("result", myInt.data.map { it + 1 })
    }

    companion object Factory : PluginFactory<MyPlugin1> {
        override val tag: PluginTag
            get() = PluginTag("Plg1")

        override val type: KClass<out MyPlugin1>
            get() = MyPlugin1::class

        override fun build(context: Context, meta: Meta): MyPlugin1 = MyPlugin1()
    }
}

internal class MyPlugin2 : WorkspacePlugin() {
    override val tag: PluginTag
        get() = Factory.tag

    val task by task<Int>(serializer()) {
        workspace.logger.info { "In ${tag.name}.task" }
        val dataSet = fromTask<Int>(Name.of(MyPlugin1.tag.name, "task"))
        val data = dataSet["result".asName()]!!
        data("result", data.map { it + 1 })
    }

    companion object Factory : PluginFactory<MyPlugin2> {
        override val tag: PluginTag
            get() = PluginTag("Plg2")

        override val type: KClass<out MyPlugin2>
            get() = MyPlugin2::class

        override fun build(context: Context, meta: Meta): MyPlugin2 = MyPlugin2()
    }
}
