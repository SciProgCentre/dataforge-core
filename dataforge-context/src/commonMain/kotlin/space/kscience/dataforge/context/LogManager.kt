package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.Named
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.plus
import kotlin.reflect.KClass

public fun interface Logger {
    public fun log(tag: String, body: () -> String)
}

public interface LogManager : Plugin, Logger {
    public fun logger(name: Name): Logger

    public val defaultLogger: Logger

    override fun log(tag: String, body: () -> String): Unit = defaultLogger.log(tag, body)

    public fun log(name: Name, tag: String, body: () -> String): Unit = logger(name).log(tag, body)

    public companion object {
        public const val TRACE: String = "TRACE"
        public const val INFO: String = "INFO"
        public const val DEBUG: String = "DEBUG"
        public const val WARNING: String = "WARNING"
        public const val ERROR: String = "ERROR"
    }
}

public fun Logger.trace(body: () -> String): Unit = log(LogManager.TRACE, body)
public fun Logger.info(body: () -> String): Unit = log(LogManager.INFO, body)
public fun Logger.debug(body: () -> String): Unit = log(LogManager.DEBUG, body)
public fun Logger.warn(body: () -> String): Unit = log(LogManager.WARNING, body)
public fun Logger.error(body: () -> String): Unit = log(LogManager.ERROR, body)

internal val (() -> String).safe: String
    get() = try {
        invoke()
    } catch (t: Throwable) {
        "Error while evaluating log string: ${t.message}"
    }


public fun Logger.error(throwable: Throwable?, body: () -> String): Unit = log(LogManager.ERROR) {
    buildString {
        appendLine(body())
        throwable?.let { appendLine(throwable.stackTraceToString()) }
    }
}


public class DefaultLogManager : AbstractPlugin(), LogManager {

    override fun logger(name: Name): Logger = Logger { tag, body ->
        val message: String = body.safe
        println("$tag $name: [${context.name}] $message")
    }

    override val defaultLogger: Logger = logger(Name.EMPTY)


    override val tag: PluginTag get() = Companion.tag

    public companion object : PluginFactory<DefaultLogManager> {
        override fun build(context: Context, meta: Meta): DefaultLogManager = DefaultLogManager()

        override val tag: PluginTag = PluginTag(group = PluginTag.DATAFORGE_GROUP, name = "log.default")
        override val type: KClass<out DefaultLogManager> = DefaultLogManager::class
    }
}

/**
 * Context log manager inherited from parent
 */
public val Context.logger: LogManager
    get() = plugins.find(inherit = true) { it is LogManager } as? LogManager
        ?: getGlobalLoggerFactory().build(context = Global, meta = Meta.EMPTY).apply { attach(Global) }

/**
 * The named proxy logger for a context member
 */
public val ContextAware.logger: Logger
    get() = if (this is Named) {
        Logger { tag, body ->
            context.logger.log(this@logger.name + name, tag, body)
        }
    } else {
        context.logger
    }

