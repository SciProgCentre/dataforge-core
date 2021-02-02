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

import hep.dataforge.meta.get
import hep.dataforge.meta.string
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

public interface GroupRule {
    public suspend fun <T : Any> gather(dataType: KClass<out T>, set: DataSet<T>): Map<String, DataSet<T>>

    public companion object {
        /**
         * Create grouping rule that creates groups for different values of value
         * field with name [key]
         *
         * @param key
         * @param defaultTagValue
         * @return
         */
        public fun byMetaValue(
            scope: CoroutineScope,
            key: String,
            defaultTagValue: String,
        ): GroupRule = object : GroupRule {

            override suspend fun <T : Any> gather(
                dataType: KClass<out T>,
                set: DataSet<T>,
            ): Map<String, DataSet<T>> {
                val map = HashMap<String, ActiveDataTree<T>>()

                set.flow().collect { data ->
                    val tagValue = data.meta[key]?.string ?: defaultTagValue
                    map.getOrPut(tagValue) { ActiveDataTree(dataType) }.emit(data.name, data.data)
                }

                scope.launch {
                    set.updates.collect { name ->
                        val data = set.getData(name)
                        val tagValue = data?.meta[key]?.string ?: defaultTagValue
                        map.getOrPut(tagValue) { ActiveDataTree(dataType) }.emit(name, data)
                    }
                }

                return map
            }
        }
    }
}
