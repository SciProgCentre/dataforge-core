/* 
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.string
import java.util.*

interface GroupRule {
    operator fun <T : Any> invoke(node: DataNode<T>): Map<String, DataNode<T>>
}

/**
 * The class to builder groups of content with annotation defined rules
 *
 * @author Alexander Nozik
 */

object GroupBuilder {

    /**
     * Create grouping rule that creates groups for different values of value
     * field with name [key]
     *
     * @param key
     * @param defaultTagValue
     * @return
     */
    fun byValue(key: String, defaultTagValue: String): GroupRule = object : GroupRule {
        override fun <T : Any> invoke(node: DataNode<T>): Map<String, DataNode<T>> {
            val map = HashMap<String, DataTreeBuilder<T>>()

            node.dataSequence().forEach { (name, data) ->
                val tagValue = data.meta[key]?.string ?: defaultTagValue
                map.getOrPut(tagValue) { DataNode.builder(node.type) }[name] = data
            }

            return map.mapValues { it.value.build() }
        }
    }


    //    @ValueDef(key = "byValue", required = true, info = "The name of annotation value by which grouping should be made")
//    @ValueDef(
//        key = "defaultValue",
//        def = "default",
//        info = "Default value which should be used for content in which the grouping value is not presented"
//    )
    fun byMeta(config: Meta): GroupRule {
        //TODO expand grouping options
        return config["byValue"]?.string?.let { byValue(it, config["defaultValue"]?.string ?: "default") }
            ?: object : GroupRule {
                override fun <T : Any> invoke(node: DataNode<T>): Map<String, DataNode<T>> = mapOf("" to node)
            }
    }
}
