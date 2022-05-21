package space.kscience.dataforge.distributed

import kotlinx.serialization.serializer
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.context.info
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.data.map
import space.kscience.dataforge.data.select
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
        val myInt = workspace.data.select<Int>()
        val res = myInt.getData("int".asName())!!
        emit("result".asName(), res.map { it + 1 })
    }

    companion object Factory : PluginFactory<MyPlugin1> {
        override fun invoke(meta: Meta, context: Context): MyPlugin1 = MyPlugin1()

        override val tag: PluginTag
            get() = PluginTag("Plg1")

        override val type: KClass<out MyPlugin1>
            get() = MyPlugin1::class
    }
}

internal class MyPlugin2 : WorkspacePlugin() {
    override val tag: PluginTag
        get() = Factory.tag

    val task by task<Int>(serializer()) {
        workspace.logger.info { "In ${tag.name}.task" }
        val dataSet = fromTask<Int>(Name.of(MyPlugin1.tag.name, "task"))
        val data = dataSet.getData("result".asName())!!
        emit("result".asName(), data.map { it + 1 })
    }

    companion object Factory : PluginFactory<MyPlugin2> {
        override fun invoke(meta: Meta, context: Context): MyPlugin2 = MyPlugin2()

        override val tag: PluginTag
            get() = PluginTag("Plg2")

        override val type: KClass<out MyPlugin2>
            get() = MyPlugin2::class
    }
}
