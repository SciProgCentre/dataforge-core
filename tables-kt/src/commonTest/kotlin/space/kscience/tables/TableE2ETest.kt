package space.kscience.tables

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TableE2ETest {

    @Test
    fun testTableEndToEndWorkflow() {
        // 1. Create a row table by filling rows manually
        val item by ColumnHeader.typed<String>()
        val price by ColumnHeader.typed<Double>()
        val quantity by ColumnHeader.typed<Int>()
        val category by ColumnHeader.typed<String>()

        val table = RowTable<Any?>(item, price, quantity, category) {
            row(item to "Apple", price to 1.5, quantity to 10, category to "Fruit")
            row(item to "Banana", price to 0.75, quantity to 20, category to "Fruit")
            row(item to "Carrot", price to 0.5, quantity to 15, category to "Vegetable")
            row(item to "Steak", price to 12.0, quantity to 4, category to "Meat")
            row(item to "Broccoli", price to 2.0, quantity to 8, category to "Vegetable")
        }

        assertEquals(5, table.rows.size)
        assertEquals("Apple", table.rows[0][item])
        assertEquals(1.5, table.rows[0][price])
        assertEquals(10, table.rows[0][quantity])
        assertEquals("Fruit", table.rows[0][category])

        // 2. Add a column computed lazily from row values
        val total by ColumnHeader.typed<Double>()

        var evaluationCount = 0
        // Create a lazy transformation column using rowsToColumn
        val lazyTotalColumn = table.rowsToColumn(total, cache = true) { row ->
            evaluationCount++
            row[price] * row[quantity]
        }

        // Verify lazy evaluation: mapper not called until column is accessed
        assertEquals(0, evaluationCount)
        assertEquals(15.0, lazyTotalColumn[0])
        assertEquals(1, evaluationCount)
        // Verify cache: subsequent access does not re-evaluate
        assertEquals(15.0, lazyTotalColumn[0])
        assertEquals(1, evaluationCount)

        // Integrate the computed column into a ColumnTable representation
        val enrichedTable = table.withColumns {
            addColumn(lazyTotalColumn)
        }

        assertEquals(5, enrichedTable.columns.size)
        assertEquals(15.0, enrichedTable[0, total])
        assertEquals(15.0, enrichedTable[1, total]) // 0.75 * 20 = 15.0
        assertEquals(7.5, enrichedTable[2, total])  // 0.5 * 15 = 7.5
        assertEquals(48.0, enrichedTable[3, total]) // 12.0 * 4 = 48.0
        assertEquals(16.0, enrichedTable[4, total]) // 2.0 * 8 = 16.0

        // 3. Create a table view from it (filter rows by category == "Vegetable" and select subset of columns)
        val vegetableRowIndices = enrichedTable.rows.indices.filter {
            enrichedTable.rows[it][category] == "Vegetable"
        }
        assertEquals(listOf(2, 4), vegetableRowIndices)

        val selectedColumns = enrichedTable.columns
            .filter { it.name in setOf("item", "quantity", "price", "total") }
            .associateBy { it }

        val view = TableView(
            origin = enrichedTable,
            rowMapping = vegetableRowIndices,
            columnMapping = selectedColumns
        )

        // Verify view structure and mapping
        assertEquals(2, view.rows.size)
        assertEquals(4, view.columns.size)

        // Row 0 of view corresponds to Carrot (origin row index 2)
        assertEquals("Carrot", view.rows[0][item])
        assertEquals(0.5, view.rows[0][price])
        assertEquals(15, view.rows[0][quantity])
        assertEquals(7.5, view.rows[0][total])

        // Row 1 of view corresponds to Broccoli (origin row index 4)
        assertEquals("Broccoli", view.rows[1][item])
        assertEquals(2.0, view.rows[1][price])
        assertEquals(8, view.rows[1][quantity])
        assertEquals(16.0, view.rows[1][total])

        // Category was excluded from columnMapping
        assertNull(view.getOrNull(0, "category"))

        // 4. Aggregate rows of resulting view
        val totalQuantity = view.rows.sumOf { it[quantity] }
        val totalCost = view.rows.sumOf { it[total] }
        val averageCost = view.rows.map { it[total] }.average()

        assertEquals(23, totalQuantity) // 15 + 8
        assertEquals(23.5, totalCost)    // 7.5 + 16.0
        assertEquals(11.75, averageCost) // 23.5 / 2

        // Aggregation via rowSequence flow
        val sequenceTotalSum = view.rowSequence().sumOf { it[total] }
        assertEquals(23.5, sequenceTotalSum)

        // Aggregation via cell indexing view[row, column]
        val indexedTotalSum = view.rows.indices.sumOf { view[it, total] ?: 0.0 }
        assertEquals(23.5, indexedTotalSum)
    }

    @Test
    fun testColumnTableTransformE2E() {
        // Alternative convenient builder approach using transform in withColumns
        val x by ColumnHeader.typed<Double>()
        val y by ColumnHeader.typed<Double>()
        val sum by ColumnHeader.typed<Double>()

        val rowTable = RowTable<Double>(x, y) {
            row(x to 1.0, y to 10.0)
            row(x to 2.0, y to 20.0)
            row(x to 3.0, y to 30.0)
        }

        val tableWithSum = rowTable.withColumns {
            transform(sum) { it[x] + it[y] }
        }

        // View selecting first and last rows
        val view = TableView(
            origin = tableWithSum,
            rowMapping = listOf(0, 2)
        )

        assertEquals(2, view.rows.size)
        assertEquals(11.0, view.rows[0][sum])
        assertEquals(33.0, view.rows[1][sum])

        val totalSum = view.rows.sumOf { it[sum] }
        assertEquals(44.0, totalSum)
    }
}
