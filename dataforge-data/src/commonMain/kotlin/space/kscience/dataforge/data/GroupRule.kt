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
package space.kscience.dataforge.data

import kotlinx.coroutines.launch
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.misc.DFInternal

public interface GroupRule {
    public fun <T : Any> gather(set: DataSet<T>): Map<String, DataSet<T>>

    public companion object {
        /**
         * Create grouping rule that creates groups for different values of value
         * field with name [key]
         *
         * @param key
         * @param defaultTagValue
         * @return
         */
        @OptIn(DFInternal::class)
        public fun byMetaValue(
            key: String,
            defaultTagValue: String,
        ): GroupRule = object : GroupRule {

            override fun <T : Any> gather(
                set: DataSet<T>,
            ): Map<String, DataSet<T>> {
                val map = HashMap<String, DataSet<T>>()

                if (set is DataSource) {
                    set.forEach { data ->
                        val tagValue: String = data.meta[key]?.string ?: defaultTagValue
                        (map.getOrPut(tagValue) { DataTreeBuilder(set.dataType, set.coroutineContext) } as DataTreeBuilder<T>)
                            .data(data.name, data.data)

                        set.launch {
                            set.updates.collect { name ->
                                val dataUpdate = set[name]

                                val updateTagValue = dataUpdate?.meta?.get(key)?.string ?: defaultTagValue
                                map.getOrPut(updateTagValue) {
                                    DataSource(set.dataType, this) {
                                        data(name, dataUpdate)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    set.forEach { data ->
                        val tagValue: String = data.meta[key]?.string ?: defaultTagValue
                        (map.getOrPut(tagValue) { StaticDataTree(set.dataType) } as StaticDataTree<T>)
                            .data(data.name, data.data)
                    }
                }


                return map
            }
        }
    }
}