package space.kscience.dataforge.exposed

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("UNCHECKED_CAST")
internal class ExposedTableTest {

    @Test
    fun exposedTable() {
        val db = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

        val table = ExposedTable<Int>(
            db,
            "test",
            listOf("a", "b", "c"),
            IntegerColumnType(),
        )

        transaction(db) {
            table.sqlTable.insert {
                it[table.sqlTable.columns.find { t -> t.name == "a" } as Column<Int>] = 42
                it[table.sqlTable.columns.find { t -> t.name == "b" } as Column<Int>] = 3
                it[table.sqlTable.columns.find { t -> t.name == "c" } as Column<Int>] = 7
            }
        }


        assertEquals(42, table.getOrNull(0, "a"))
        assertEquals(3, table.getOrNull(0, "b"))
        assertEquals(7, table.getOrNull(0, "c"))
        assertEquals(3, table.columns.size)
        table.columns.forEach { assertEquals(1, it.size) }
        assertEquals(1, table.rows.size)
    }
}
