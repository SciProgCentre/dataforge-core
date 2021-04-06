package space.kscience.dataforge.context

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.Named
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.plus
import kotlin.reflect.KClass

public fun interface Logable {
    public fun log(name: Name, tag: String, body: () -> String)
}

public interface LogManager : Plugin, Logable {
    public companion object {
        public const val TRACE: String = "TRACE"
        public const val INFO: String = "INFO"
        public const val DEBUG: String = "DEBUG"
        public const val WARNING: String = "WARNING"
        public const val ERROR: String = "ERROR"
    }
}

public fun Logable.trace(name: Name = Name.EMPTY, body: () -> String): Unit = log(name, LogManager.TRACE, body)
public fun Logable.info(name: Name = Name.EMPTY, body: () -> String): Unit = log(name, LogManager.INFO, body)
public fun Logable.debug(name: Name = Name.EMPTY, body: () -> String): Unit = log(name, LogManager.DEBUG, body)
public fun Logable.warn(name: Name = Name.EMPTY, body: () -> String): Unit = log(name, LogManager.WARNING, body)
public fun Logable.error(name: Name = Name.EMPTY, body: () -> String): Unit = log(name, LogManager.ERROR, body)

internal val (() -> String).safe: String
    get() = try {
        invoke()
    } catch (t: Throwable) {
        "Error while evaluating log string: ${t.message}"
    }


public fun Logable.error(throwable: Throwable?, name: Name = Name.EMPTY, body: () -> String): Unit =
    log(name, LogManager.ERROR) {
        buildString {
            appendLine(body())
            throwable?.let { appendLine(throwable.stackTraceToString()) }
        }
    }


public class DefaultLogManager : AbstractPlugin(), LogManager {

    override fun log(name: Name, tag: String, body: () -> String) {
        val message: String = body.safe
        println("[${context.name}] $name: $message")
    }

    override val tag: PluginTag get() = Companion.tag

    public companion object : PluginFactory<DefaultLogManager> {
        override fun invoke(meta: Meta, context: Context): DefaultLogManager = DefaultLogManager()

        override val tag: PluginTag = PluginTag(group = PluginTag.DATAFORGE_GROUP, name = "log.default")
        override val type: KClass<out DefaultLogManager> = DefaultLogManager::class
    }
}

/**
 * Context log manager inherited from parent
 */
public val Context.logger: Logable
    get() = plugins.find(inherit = true) { it is LogManager } as? LogManager
        ?: globalLoggerFactory(context = Global).apply { attach(Global) }

/**
 * The named proxy logger for a context member
 */
public val ContextAware.logger: Logable
    get() = if (this is Named) {
        object : Logable {
            val contextLog = context.logger
            override fun log(name: Name, tag: String, body: () -> String) {
                contextLog.log(this@logger.name + name, tag, body)
            }
        }
    } else {
        context.logger
    }

