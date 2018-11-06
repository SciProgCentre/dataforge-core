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

import java.util.*
import kotlin.collections.HashMap


private fun Properties.asMeta(): Meta {
    return buildMeta {
        this@asMeta.forEach { key, value ->
            set(key.toString().toName(), value)
        }
    }
}

/**
 * A singleton global context. Automatic root for the whole context hierarchy. Also stores the registry for active contexts.
 *
 * @author Alexander Nozik
 */
actual object Global : Context, JVMContext("GLOBAL", null, Thread.currentThread().contextClassLoader) {

    /**
     * Closing all contexts
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    override fun close() {
        logger.info("Shutting down GLOBAL")
        for (ctx in contextRegistry.values) {
            ctx.close()
        }
        super.close()
    }

    private val contextRegistry = HashMap<String, Context>()

    /**
     * Get previously builder context o builder a new one
     *
     * @param name
     * @return
     */
    @Synchronized
    fun getContext(name: String): Context {
        return contextRegistry.getOrPut(name) { JVMContext(name) }
    }

    /**
     * Close all contexts and terminate framework
     */
    @JvmStatic
    fun terminate() {
        try {
            close()
        } catch (e: Exception) {
            logger.error("Exception while terminating DataForge framework", e)
        }

    }
}
