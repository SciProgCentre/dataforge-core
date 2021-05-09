package space.kscience.dataforge.exposed

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.tables.Column
import space.kscience.dataforge.tables.Row
import space.kscience.dataforge.tables.Table
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import org.jetbrains.exposed.sql.Column as SqlColumn

public class ExposedColumn<T : Any>(
    public val db: Database,
    public val sqlTable: IntIdTable,
    public val sqlColumn: SqlColumn<T>,
    public override val type: KType,
) : Column<T> {
    public override val name: String
        get() = sqlColumn.name

    public override val meta: Meta
        get() = Meta.EMPTY

    public override val size: Int
        get() = transaction(db) { sqlColumn.table.selectAll().count().toInt() }

    public override fun get(index: Int): T? = transaction(db) {
        sqlTable.select { sqlTable.id eq index + 1 }.firstOrNull()?.getOrNull(sqlColumn)
    }
}

@Suppress("UNCHECKED_CAST")
public class ExposedRow<T : Any>(
    public val db: Database,
    public val sqlTable: IntIdTable,
    public val sqlRow: ResultRow,
) :
    Row<T> {
    public override fun get(column: String): T? = transaction(db) {
        val theColumn = sqlTable.columns.find { it.name == column } as SqlColumn<T>? ?: return@transaction null
        sqlRow.getOrNull(theColumn)
    }
}

@Suppress("UNCHECKED_CAST")
public class ExposedTable<T : Any>(public val db: Database, public val sqlTable: IntIdTable, public val type: KType) :
    Table<T> {
    public override val columns: List<Column<T>> =
        sqlTable.columns.filterNot { it.name == "id" }.map { ExposedColumn(db, sqlTable, it as SqlColumn<T>, type) }

    public override val rows: List<Row<T>>
        get() = transaction(db) {
            sqlTable.selectAll().map { ExposedRow(db, sqlTable, it) }
        }

    public override operator fun get(row: Int, column: String): T? = transaction(db) {
        val sqlColumn: SqlColumn<T> = sqlTable.columns.find { it.name == column } as SqlColumn<T>?
            ?: return@transaction null

        sqlTable.select { sqlTable.id eq row + 1 }.firstOrNull()?.getOrNull(sqlColumn)
    }
}

public inline fun <reified T : Any> ExposedTable(db: Database, table: IntIdTable): ExposedTable<T> =
    ExposedTable(db, table, typeOf<T>())

public inline fun <reified T : Any> ExposedTable(
    db: Database,
    tableName: String,
    columns: List<String>,
    sqlColumnType: IColumnType,
): ExposedTable<T> {
    val table = object : IntIdTable(tableName) {
        init {
            columns.forEach { registerColumn<T>(it, sqlColumnType) }
        }
    }

    transaction(db) { SchemaUtils.createMissingTablesAndColumns(table) }
    return ExposedTable(db, table)
}
