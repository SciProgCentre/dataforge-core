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
package space.kscience.dataforge.context

import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.cast

public class ClassLoaderPlugin(private val classLoader: ClassLoader) : AbstractPlugin() {
    override val tag: PluginTag = PluginTag("classLoader", PluginTag.DATAFORGE_GROUP)

    private val serviceCache: MutableMap<Class<*>, ServiceLoader<*>> = HashMap()

    public fun <T : Any> services(type: KClass<T>): Sequence<T> {
        return serviceCache.getOrPut(type.java) { ServiceLoader.load(type.java, classLoader) }.asSequence()
            .map { type.cast(it) }
    }

    public companion object {
        public val DEFAULT: ClassLoaderPlugin = ClassLoaderPlugin(Global::class.java.classLoader)
    }
}

public val Context.classLoaderPlugin: ClassLoaderPlugin get() = this.plugins.get() ?: ClassLoaderPlugin.DEFAULT

public inline fun <reified T : Any> Context.services(): Sequence<T> = classLoaderPlugin.services(T::class)


//open class JVMContext(
//    final override val name: String,
//    final override val parent: JVMContext? = Global,
//    classLoader: ClassLoader? = null,
//    properties: Meta = EmptyMeta
//) : Context, AutoCloseable {
//
//    override val properties: Meta = if (parent == null) {
//        properties
//    } else {
//        Laminate(properties, parent.properties)
//    }
//
//    override val plugins: PluginManager by lazy { PluginManager(this) }
//    override val logger: KLogger = KotlinLogging.logger(name)
//
//    /**
//     * A class loader for this context. Parent class loader is used by default
//     */
//    open val classLoader: ClassLoader = classLoader ?: parent?.classLoader ?: Global.classLoader
//
//    /**
//     * A property showing that dispatch thread is started in the context
//     */
//    private var started = false
//
//    /**
//     * A dispatch thread executor for current context
//     *
//     * @return
//     */
//    val dispatcher: ExecutorService by lazy {
//        logger.info("Initializing dispatch thread executor in {}", name)
//        Executors.newSingleThreadExecutor { r ->
//            Thread(r).apply {
//                priority = 8 // slightly higher priority
//                isDaemon = true
//                name = this@JVMContext.name + "_dispatch"
//            }.also { started = true }
//        }
//    }
//
//    private val serviceCache: MutableMap<Class<*>, ServiceLoader<*>> = HashMap()
//
//    fun <T : Any> services(type: KClass<T>): Sequence<T> {
//        return serviceCache.getOrPut(type.java) { ServiceLoader.load(type.java, classLoader) }.asSequence()
//            .map { type.cast(it) }
//    }
//
//    /**
//     * Free up resources associated with this context
//     *
//     * @throws Exception
//     */
//    override fun close() {
//        if (isActive) error("Can't close active context")
//        //detach all plugins
//        plugins.forEach { it.detach() }
//
//        if (started) {
//            dispatcher.shutdown()
//        }
//    }
//
//    private val activators = HashSet<WeakReference<Any>>()
//
//    override val isActive: Boolean = activators.all { it.get() == null }
//
//    override fun activate(activator: Any) {
//        activators.add(WeakReference(activator))
//    }
//
//    override fun deactivate(activator: Any) {
//        activators.removeAll { it.get() == activator }
//    }
//}
//
