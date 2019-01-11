/*
 * Copyright  2018 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hep.dataforge.context

import hep.dataforge.meta.*
import kotlinx.coroutines.Dispatchers
import mu.KLogger
import mu.KotlinLogging
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.HashSet
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

open class JVMContext(
        final override val name: String,
        final override val parent: JVMContext? = Global,
        classLoader: ClassLoader? = null,
        properties: Meta = EmptyMeta
) : Context, AutoCloseable {

    private val _properties = Config().apply { update(properties) }
    override val properties: Meta
        get() = if (parent == null) {
            _properties
        } else {
            Laminate(_properties, parent.properties)
        }

    override val plugins: PluginManager by lazy { PluginManager(this) }
    override val logger: KLogger = KotlinLogging.logger(name)

    /**
     * A class loader for this context. Parent class loader is used by default
     */
    open val classLoader: ClassLoader = classLoader ?: parent?.classLoader ?: Global.classLoader

    /**
     * A property showing that dispatch thread is started in the context
     */
    private var started = false

    /**
     * A dispatch thread executor for current context
     *
     * @return
     */
    val dispatcher: ExecutorService by lazy {
        logger.info("Initializing dispatch thread executor in {}", name)
        Executors.newSingleThreadExecutor { r ->
            Thread(r).apply {
                priority = 8 // slightly higher priority
                isDaemon = true
                name = this@JVMContext.name + "_dispatch"
            }.also { started = true }
        }
    }

    private val serviceCache: MutableMap<Class<*>, ServiceLoader<*>> = HashMap()

    fun <T : Any> services(type: KClass<T>): Sequence<T> {
        return serviceCache.getOrPut(type.java) { ServiceLoader.load(type.java, classLoader) }.asSequence().map { type.cast(it) }
    }

    /**
     * Free up resources associated with this context
     *
     * @throws Exception
     */
    override fun close() {
        if (isActive) error("Can't close active context")
        //detach all plugins
        plugins.forEach { it.detach() }

        if (started) {
            dispatcher.shutdown()
        }
    }

    private val activators = HashSet<WeakReference<Any>>()

    override val isActive: Boolean = activators.all { it.get() == null }

    override fun activate(activator: Any) {
        activators.add(WeakReference(activator))
    }

    override fun deactivate(activator: Any) {
        activators.removeAll { it.get() == activator }
    }
}

