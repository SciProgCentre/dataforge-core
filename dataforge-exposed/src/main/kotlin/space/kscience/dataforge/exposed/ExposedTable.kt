package space.kscience.dataforge.exposed

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

/**
 * Exposed based [Column] implementation.
 *
 * @param T The type of table items.
 * @property db The Exposed database.
 * @param sqlTable The Exposed table, which must follow the properties defined for [ExposedTable.sqlTable].
 * @param sqlColumn The Exposed column.
 * @param type The type of [T].
 */
public class ExposedColumn<T : Any>(
    public val db: Database,
    public val sqlTable: IntIdTable,
    public val sqlColumn: SqlColumn<T>,
    public override val type: KType,
) : Column<T> {
    /**
     * The name of this column.
     */
    public override val name: String
        get() = sqlColumn.name

    /**
     * Returns [Meta.EMPTY] because it is impossible to store metadata correctly with SQL columns.
     */
    public override val meta: Meta
        get() = Meta.EMPTY

    /**
     * Returns the count of rows in the table.
     */
    public override val size: Int
        get() = transaction(db) { sqlColumn.table.selectAll().count().toInt() }

    /**
     * Acquires the value of this column in the row [index].
     */
    public override fun get(index: Int): T? = transaction(db) {
        sqlTable.select { sqlTable.id eq index + 1 }.firstOrNull()?.getOrNull(sqlColumn)
    }
}

/**
 * Exposed based [Row] implementation.
 *
 * @param T The type of table items.
 * @param db The Exposed database.
 * @param sqlTable The Exposed table, which must follow the properties defined for [ExposedTable.sqlTable].
 * @param sqlRow The Exposed row.
 */
@Suppress("UNCHECKED_CAST")
public class ExposedRow<T : Any>(
    public val db: Database,
    public val sqlTable: IntIdTable,
    public val sqlRow: ResultRow,
) :
    Row<T> {
    /**
     * Acquires the value of [column] in this row.
     */
    public override fun get(column: String): T? = transaction(db) {
        val theColumn = sqlTable.columns.find { it.name == column } as SqlColumn<T>? ?: return@transaction null
        sqlRow.getOrNull(theColumn)
    }
}

/**
 * Exposed based [Table] implementation.
 *
 * @property db The Exposed database.
 *
 * @property sqlTable The Exposed table. It must have the following properties:
 * 1. Integer `id` column must be present with auto-increment by sequence 1, 2, 3&hellip;
 * 1. All other columns must be of type [T].
 *
 * @property type The type of [T].
 */
@Suppress("UNCHECKED_CAST")
public class ExposedTable<T : Any>(public val db: Database, public val sqlTable: IntIdTable, public val type: KType) :
    Table<T> {
    /**
     * The list of columns in this table.
     */
    public override val columns: List<ExposedColumn<T>> =
        sqlTable.columns.filterNot { it.name == "id" }.map { ExposedColumn(db, sqlTable, it as SqlColumn<T>, type) }

    /**
     * The list of rows in this table.
     */
    public override val rows: List<ExposedRow<T>>
        get() = transaction(db) {
            sqlTable.selectAll().map { ExposedRow(db, sqlTable, it) }
        }

    public override operator fun get(row: Int, column: String): T? = transaction(db) {
        val sqlColumn: SqlColumn<T> = sqlTable.columns.find { it.name == column } as SqlColumn<T>?
            ?: return@transaction null

        sqlTable.select { sqlTable.id eq row + 1 }.firstOrNull()?.getOrNull(sqlColumn)
    }
}

/**
 * Constructs [ExposedTable].
 *
 * @param T The type of table items.
 * @param db The Exposed database.
 * @param sqlTable The Exposed table, which must follow the properties defined for [ExposedTable.sqlTable].
 * @return A new [ExposedTable].
 */
public inline fun <reified T : Any> ExposedTable(db: Database, sqlTable: IntIdTable): ExposedTable<T> =
    ExposedTable(db, sqlTable, typeOf<T>())

/**
 * Constructs [ExposedTable].
 *
 * @param T The type of table items.
 * @param db The Exposed database.
 * @param tableName The name of table.
 * @param columns The list of columns' names.
 * @param sqlColumnType The [IColumnType] for [T].
 * @return A new [ExposedTable].
 */
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
