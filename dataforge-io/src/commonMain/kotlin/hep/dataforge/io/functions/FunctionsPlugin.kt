package hep.dataforge.io.functions

import hep.dataforge.context.AbstractPlugin
import hep.dataforge.context.PluginFactory
import hep.dataforge.context.PluginTag
import hep.dataforge.io.DoubleIOFormat
import hep.dataforge.io.IOFormat
import hep.dataforge.io.IOPlugin
import hep.dataforge.meta.Meta
import hep.dataforge.meta.buildMeta
import kotlin.reflect.KClass

class FunctionsPlugin(meta: Meta) : AbstractPlugin(meta) {
    override val tag: PluginTag get() = Companion.tag

    override fun dependsOn(): List<PluginFactory<*>> = listOf(IOPlugin)

    private val specs: Collection<FunctionSpec<out Any, out Any>> = listOf(
        DoubleToDoubleFunctionSpec
    )

    fun resolve(meta: Meta): FunctionSpec<*, *>? {
        return specs.find { it.toMeta() == meta }
    }

//    fun <T : Any, R : Any> resolve(inputType: KClass<out T>, outputType: KClass<out R>): FunctionSpec<T, R> {
//
//    }

    companion object : PluginFactory<FunctionsPlugin> {

        override val tag: PluginTag = PluginTag("io.functions", group = PluginTag.DATAFORGE_GROUP)
        override val type: KClass<out FunctionsPlugin> = FunctionsPlugin::class
        override fun invoke(meta: Meta): FunctionsPlugin = FunctionsPlugin(meta)
    }
}

object DoubleToDoubleFunctionSpec : FunctionSpec<Double, Double> {
    override val inputFormat: IOFormat<Double> get() = DoubleIOFormat
    override val outputFormat: IOFormat<Double> get() = DoubleIOFormat

    override fun toMeta(): Meta = buildMeta {
        "input" to "Double"
        "output" to "Double"
    }
}

//suspend inline fun <reified T : Any, reified R : Any> FunctionServer.call(name: String, arg: T): R {
//    val plugin = context.plugins.load(FunctionsPlugin)
//    val spec = plugin.resolve(T::class, R::class)
//    return call(name, spec, arg)
//}
//
//inline operator fun <reified T : Any, reified R : Any> FunctionServer.get(name: String): (suspend (T) -> R) {
//    val plugin = context.plugins.load(FunctionsPlugin)
//    val spec = plugin.resolve(T::class, R::class)
//    return get(name, spec)
//}