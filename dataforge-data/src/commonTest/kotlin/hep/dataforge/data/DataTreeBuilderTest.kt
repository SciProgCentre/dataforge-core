package hep.dataforge.data

import kotlin.test.Test
import kotlin.test.assertTrue


internal class DataTreeBuilderTest{
    @Test
    fun testDataUpdate(){
        val updateData = DataNode<Any>{
            "update" to {
                "a" to Data.static("a")
                "b" to Data.static("b")
            }
        }

        val node = DataNode<Any>{
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