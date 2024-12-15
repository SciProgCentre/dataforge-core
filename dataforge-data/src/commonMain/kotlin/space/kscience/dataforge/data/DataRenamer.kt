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

import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.misc.UnsafeKType
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.plus
import kotlin.reflect.KType

/**
 * Interface that define rename rule for [Data]
 */
@DFExperimental
public fun interface DataRenamer {
    public fun rename(name: Name, meta: Meta, type: KType): Name

    public companion object {

        /**
         * Prepend name token `key\[tagValue\]` to data name
         */
        @OptIn(UnsafeKType::class)
        public fun groupByMetaValue(
            key: String,
            defaultTagValue: String,
        ): DataRenamer = object : DataRenamer {

            override fun rename(
                name: Name,
                meta: Meta,
                type: KType
            ): Name {
                val tagValue: String = meta[key]?.string ?: defaultTagValue
                return NameToken(key,tagValue).plus(name)
            }
        }
    }
}