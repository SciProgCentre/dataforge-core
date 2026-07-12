package space.kscience.dataforge.data

import kotlinx.coroutines.test.runTest
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.last
import kotlin.reflect.typeOf
import kotlin.test.*

class FilteredDataTreeTest {


    @Test
    fun testAcceptsAll() = runTest {
        // Define DataTree structure
        val dataTree = DataTree<String> {
            value("root")
            node("childToken") {
                value("child")
            }
        }
        val filter = DataFilter.EMPTY

        // Create FilteredDataTree
        val filteredTree = dataTree.filterData(filter)

        // Test filtered data
        assertNotNull(filteredTree.data)
        assertEquals("root", filteredTree.data?.await())
        assertTrue { filteredTree.items.containsKey(NameToken("childToken")) }
    }

    @Test
    fun testRejectSpecificType() {
        // Define filter to reject all data of type Int
        val filter = DataFilter { _, _, type -> type != typeOf<Int>() }

        val dataTree = DataTree<Int> {
            value(42)
        }

        // Create FilteredDataTree
        val filteredTree = dataTree.filterData(filter)

        // Test filtered data
        assertNull(filteredTree.data)
    }

    @Test
    fun testRejectByName() {
        // Define a name-based filter rejecting everything under "rejectedToken"
        val filter = DataFilter { name, _, _ -> !name.toString().contains("rejectedToken") }

        val dataTree = DataTree<String> {
            node("rejectedToken") {
                value("rejected")
            }
            node("acceptedToken") {
                value("accepted")
            }
        }

        // Create FilteredDataTree
        val filteredTree = dataTree.filterData(filter)

        // Test filtered data
        assertFalse(filteredTree.items.containsKey(NameToken("rejectedToken")))
        assertTrue(filteredTree.items.containsKey(NameToken("acceptedToken")))
    }

    @Test
    fun testNestedFiltering() {
        // Define a filter allowing only names containing "allowed"
        val filter = DataFilter { name, _, _ -> name.last().body == "allowed"}

        val dataTree = DataTree<String> {
            node("allowed") {
                value("child")
                node("notAllowedChild") {
                    value("notAllowedChild")
                }
            }
            node("notAllowed") {
                value("notAllowed")
            }
        }

        // Create FilteredDataTree
        val filteredTree = dataTree.filterData(filter)

        // Test nested filtered items
        assertTrue(filteredTree.items.containsKey(NameToken("allowed")))
        assertFalse(filteredTree.items.containsKey(NameToken("notAllowed")))

        val filteredChildTree = filteredTree.branch("allowed")
        assertNotNull(filteredChildTree)
        assertTrue(filteredChildTree.items.isEmpty())
    }

    @Test
    fun testAcceptsMeta() = runTest {
        // Define a filter based on metadata
        val filter = DataFilter { _, meta, _ -> meta?.get("key").string == "value" }

        val dataTree = DataTree<String> {
            value("accepted", Meta { "key" put "value" })
            node("child") {
                value("rejected", Meta { "key" put "otherValue" })
            }
        }

        // Create FilteredDataTree
        val filteredTree = dataTree.filterData(filter)

        // Test filtering by metadata
        assertNotNull(filteredTree.data)
        assertEquals("accepted", filteredTree.data?.await())
        assertTrue(filteredTree.items.isEmpty())
    }
}