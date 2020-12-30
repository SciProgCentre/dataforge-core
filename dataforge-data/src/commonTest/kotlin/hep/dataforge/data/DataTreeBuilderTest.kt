package hep.dataforge.data

import kotlin.test.Test
import kotlin.test.assertTrue


internal class DataTreeBuilderTest{
    @Test
    fun testDataUpdate(){
        val updateData = DataTree<Any>{
            "update" put {
                "a" put Data.static("a")
                "b" put Data.static("b")
            }
        }

        val node = DataTree<Any>{
            node("primary"){
                static("a","a")
                static("b","b")
            }
            static("root","root")
            update(updateData)
        }

        println(node.toMeta())

        assertTrue { node["update.a"] != null }
        assertTrue { node["primary.a"] != null }

    }
}