package hep.dataforge.context

import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import kotlin.reflect.KClass


public class NativeLogManager : AbstractPlugin(), LogManager {

    override fun log(name: Name, tag: String, body: () -> String) {
        val text = try {
            body()
        } catch (t: Throwable){
            "Error while evaluating log string: ${t.message}"
        }
        println("[${context.name}] $name: $text")
    }

    override val tag: PluginTag get() = Companion.tag

    public companion object : PluginFactory<NativeLogManager> {
        override fun invoke(meta: Meta, context: Context): NativeLogManager = NativeLogManager()

        override val tag: PluginTag = PluginTag(group = PluginTag.DATAFORGE_GROUP, name = "log.native")
        override val type: KClass<out NativeLogManager> = NativeLogManager::class
    }
}

internal actual val globalLogger: LogManager = NativeLogManager().apply { attach(Global) }
