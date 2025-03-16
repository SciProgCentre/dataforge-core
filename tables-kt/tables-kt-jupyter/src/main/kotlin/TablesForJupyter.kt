package space.kscience.tables

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.kotlinx.jupyter.api.HTML
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration

private const val MAX_ROWS = 20


public class TablesForJupyter : JupyterIntegration() {

    private fun TagConsumer<*>.appendHeaders(headers: TableHeader<*>){
        tr {
            classes = classes + "tables-kt-header"
            headers.forEach { column ->
                th {
                    +column.name
                }
            }
        }
    }


    private fun TagConsumer<*>.appendRowValues(headers: TableHeader<*>, row: Row<*>){
        tr {
            classes = classes + "tables-kt-row"
            headers.forEach { column ->
                td {
                    +row[column].toString()
                }
            }
        }
    }

    override fun Builder.onLoaded() {
        repositories("https://repo.kotlin.link")

        import(
            "space.kscience.tables.*",
            "space.kscience.dataforge.meta.*",
            "space.kscience.dataforge.values.*"
            //"space.kscience.tables.io.*",
        )

        //TODO replace by advanced widget
        render<Table<*>> { table ->
            HTML(
                createHTML().table {
                    classes = classes + "tables-kt-table"
                    consumer.appendHeaders(table.headers)
                    table.rows.take(MAX_ROWS).forEach { row ->
                        consumer.appendRowValues(table.headers, row)
                    }
                    if (table.rows.size > MAX_ROWS) {
                        tr {
                            td {
                                +"... Displaying first 20 of ${table.rows.size} rows ..."
                            }
                        }
                    }
                }
            )
        }

        render<Column<*>> { column ->
            HTML(
                createHTML().table {
                    classes = classes + "tables-kt-table"
                    tr {
                        classes = classes + "tables-kt-header"
                        th {
                            +column.name
                        }
                    }
                    column.sequence().take(MAX_ROWS).forEach { value ->
                        tr {
                            classes = classes + "tables-kt-row"
                            td {
                                +value.toString()
                            }
                        }
                    }
                    if (column.size > MAX_ROWS) {
                        tr {
                            td {
                                +"... Displaying first 20 of ${column.size} values ..."
                            }
                        }
                    }
                }
            )
        }

    }
}