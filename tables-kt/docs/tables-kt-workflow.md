# Tables.kt Core End-to-End Workflow Guide

This document presents a complete end-to-end example and guide for the core features of `tables-kt`. It demonstrates how to create row-based tables, compute lazy columns from existing row data, project and filter data using table views, and perform row aggregations on the resulting views.

---

## Workflow Overview

The core workflow consists of four key stages:

1. **Creating a Row Table**: Constructing a `RowTable` and populating rows manually using type-safe column headers and DSL builders.
2. **Adding a Lazily Computed Column**: Creating virtual/computed columns using `rowsToColumn` (or `transform`) with optional value caching.
3. **Creating a Table View**: Constructing a `TableView` over the table to filter or reorder rows and select specific columns without copying data.
4. **Aggregating Rows**: Performing reductions and calculations (such as sums, averages, and sequence folds) on the view's rows.

---

## 1. Creating a Row Table

`RowTable` represents a table stored row-wise. You can build one using the `RowTable` builder DSL and typed column headers:

```kotlin
import space.kscience.tables.*

// Define typed column headers
val item by ColumnHeader.typed<String>()
val price by ColumnHeader.typed<Double>()
val quantity by ColumnHeader.typed<Int>()
val category by ColumnHeader.typed<String>()

// Construct the table
val table = RowTable<Any?>(item, price, quantity, category) {
    row(item to "Apple", price to 1.5, quantity to 10, category to "Fruit")
    row(item to "Banana", price to 0.75, quantity to 20, category to "Fruit")
    row(item to "Carrot", price to 0.5, quantity to 15, category to "Vegetable")
    row(item to "Steak", price to 12.0, quantity to 4, category to "Meat")
    row(item to "Broccoli", price to 2.0, quantity to 8, category to "Vegetable")
}
```

### Type-Safe Row Access
Rows implement `Row<T>` and support typed lookup:
```kotlin
val firstRow = table.rows[0]
val itemName: String = firstRow[item]       // "Apple"
val itemPrice: Double = firstRow[price]     // 1.5
val itemQuantity: Int = firstRow[quantity]  // 10
```

---

## 2. Adding a Lazily Computed Column

Columns can be derived from row values on-demand using `rowsToColumn`. When `cache = true`, computed values are stored upon first evaluation and reused on subsequent lookups.

```kotlin
val total by ColumnHeader.typed<Double>()

// Create a cached lazy transformation column
val lazyTotalColumn = table.rowsToColumn(total, cache = true) { row ->
    row[price] * row[quantity]
}

// Attach the column to a ColumnTable representation
val enrichedTable = table.withColumns {
    addColumn(lazyTotalColumn)
}
```

Alternatively, you can compute columns inline using the `transform` builder function:
```kotlin
val enrichedTable = table.withColumns {
    transform(total) { row -> row[price] * row[quantity] }
}
```

---

## 3. Creating a Table View

`TableView` provides a zero-copy read-only view over an existing `Table`. It allows you to:
- **Filter and reorder rows** via `rowMapping: List<Int>` (indices pointing into the origin table).
- **Select, reorder, or alias columns** via `columnMapping: Map<Column<T>, Column<T>>`.

```kotlin
// Filter rows to include only "Vegetable" category
val vegetableRowIndices = enrichedTable.rows.indices.filter {
    enrichedTable.rows[it][category] == "Vegetable"
} // Yields [2, 4]

// Select specific visible columns (e.g., exclude "category")
val selectedColumns = enrichedTable.columns
    .filter { it.name in setOf("item", "quantity", "price", "total") }
    .associateBy { it }

// Create the view
val view = TableView(
    origin = enrichedTable,
    rowMapping = vegetableRowIndices,
    columnMapping = selectedColumns
)
```

In this view:
- `view.rows.size` is `2` (corresponding to Carrot and Broccoli).
- `view.rows[0][item]` accesses row index `2` in `enrichedTable` ("Carrot").
- `view.rows[1][item]` accesses row index `4` in `enrichedTable` ("Broccoli").
- Excluded columns (such as `category`) return `null`.

---

## 4. Aggregating Rows of the View

You can compute statistics and reductions across view rows using standard Kotlin collection operations or sequences:

```kotlin
// Aggregate across rows
val totalQuantity = view.rows.sumOf { it[quantity] } // 15 + 8 = 23
val totalCost = view.rows.sumOf { it[total] }         // 7.5 + 16.0 = 23.5
val averageCost = view.rows.map { it[total] }.average() // 11.75

// Streaming aggregation over rowSequence
val sequenceTotalSum = view.rowSequence().sumOf { it[total] } // 23.5

// Aggregation via cell indexing
val indexedTotalSum = view.rows.indices.sumOf { view[it, total] ?: 0.0 } // 23.5
```

---

## Full End-to-End Example

Below is the complete, runnable example matching the test suite (`TableE2ETest.kt`):

```kotlin
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

        // 2. Add a column computed lazily from row values
        val total by ColumnHeader.typed<Double>()
        val lazyTotalColumn = table.rowsToColumn(total, cache = true) { row ->
            row[price] * row[quantity]
        }

        val enrichedTable = table.withColumns {
            addColumn(lazyTotalColumn)
        }

        // 3. Create a table view (filter vegetables & project columns)
        val vegetableRowIndices = enrichedTable.rows.indices.filter {
            enrichedTable.rows[it][category] == "Vegetable"
        }

        val selectedColumns = enrichedTable.columns
            .filter { it.name in setOf("item", "quantity", "price", "total") }
            .associateBy { it }

        val view = TableView(
            origin = enrichedTable,
            rowMapping = vegetableRowIndices,
            columnMapping = selectedColumns
        )

        // 4. Aggregate rows of resulting view
        val totalQuantity = view.rows.sumOf { it[quantity] }
        val totalCost = view.rows.sumOf { it[total] }
        val averageCost = view.rows.map { it[total] }.average()

        assertEquals(23, totalQuantity)
        assertEquals(23.5, totalCost)
        assertEquals(11.75, averageCost)
    }
}
```
