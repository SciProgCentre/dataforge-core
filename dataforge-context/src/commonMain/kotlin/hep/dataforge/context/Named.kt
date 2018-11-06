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
package hep.dataforge.context

/**
 * Any object that have name
 *
 * @author Alexander Nozik
 */
interface Named {

    /**
     * The name of this object instance
     *
     * @return
     */
    val name: String

    companion object {
        const val ANONYMOUS = ""

        /**
         * Get the name of given object. If object is Named its name is used,
         * otherwise, use Object.toString
         *
         * @param obj
         * @return
         */
        fun nameOf(obj: Any): String {
            return if (obj is Named) {
                obj.name
            } else {
                obj.toString()
            }
        }
    }
}

/**
 * Check if this object has an empty name and therefore is anonymous.
 * @return
 */
val Named.isAnonymous: Boolean
    get() = this.name == Named.ANONYMOUS
