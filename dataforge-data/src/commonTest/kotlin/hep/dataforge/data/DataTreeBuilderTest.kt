package hep.dataforge.data

import kotlin.test.Test
import kotlin.test.assertTrue


internal class DataTreeBuilderTest{
    @Test
    fun testDataUpdate(){
        val updateData = DataNode.build(Any::class){
            "update" to {
                "a" to Data.static("a")
                "b" to Data.static("b")
            }
        }

        val node = DataNode.build(Any::class){
            "primary" to {
                "a" to Data.static("a")
                "b" to Data.static("b")
            }
            "root" to Data.static("root")
            update(updateData)
        }

        println(node.toMeta())

        assertTrue { node["update.a"] != null }
        assertTrue { node["primary.a"] != null }

    }
}