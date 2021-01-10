package hep.dataforge.data

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue


internal class DataTreeBuilderTest{
    @Test
    fun testDataUpdate(){
        val updateData: DataTree<Any> = DataTree.static{
            "update" put {
                "a" put Data.static("a")
                "b" put Data.static("b")
            }
        }

        val node = DataTree.static<Any>{
            set("primary"){
                data("a","a")
                data("b","b")
            }
            data("root","root")
            runBlocking {
                update(updateData)
            }
        }


        assertTrue { node.branch("update.a") != null }
        assertTrue { node.branch("primary.a") != null }

    }
}