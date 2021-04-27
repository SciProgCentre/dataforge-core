package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.Name
import kotlin.reflect.KClass

public class ConsoleLogManager : AbstractPlugin(), LogManager {

    override fun logger(name: Name): Logger  = Logger { tag, body ->
        val message: String = body.safe
        when (tag) {
            LogManager.INFO -> console.info("[${context.name}] $name: $message")
            LogManager.WARNING ->  console.warn("[${context.name}] $name: $message")
            LogManager.ERROR -> console.error("[${context.name}] $name: $message")
            else ->  console.log("[${context.name}] $name: $message")
        }
    }

    override val defaultLogger: Logger = logger(Name.EMPTY)


    override val tag: PluginTag get() = Companion.tag

    public companion object : PluginFactory<ConsoleLogManager> {
        override fun invoke(meta: Meta, context: Context): ConsoleLogManager = ConsoleLogManager()

        override val tag: PluginTag = PluginTag(group = PluginTag.DATAFORGE_GROUP, name = "log.jsConsole")
        override val type: KClass<out ConsoleLogManager> = ConsoleLogManager::class
    }
}

internal actual val globalLoggerFactory: PluginFactory<out LogManager> = ConsoleLogManager
