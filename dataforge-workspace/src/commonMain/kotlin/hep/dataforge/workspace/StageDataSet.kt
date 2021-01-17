package hep.dataforge.workspace

import hep.dataforge.data.DataSet
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A result of a [WorkStage]
 */
public interface StageDataSet<out T : Any> : DataSet<T> {
    /**
     * The [Workspace] this [DataSet] belongs to
     */
    public val workspace: Workspace

    /**
     * The [Name] of the stage that produced this [DataSet]
     */
    public val stageName: Name

    /**
     * The configuration of the stage that produced this [DataSet]
     */
    public val stageMeta: Meta

    override fun flow(): Flow<StageData<T>>
    override suspend fun getData(name: Name): StageData<T>?
}

private class StageDataSetImpl<out T : Any>(
    override val workspace: Workspace,
    val dataSet: DataSet<T>,
    override val stageName: Name,
    override val stageMeta: Meta,
) : StageDataSet<T>, DataSet<T> by dataSet {

    override fun flow(): Flow<StageData<T>> = dataSet.flow().map {
        workspace.internalize(it, it.name, stageName, stageMeta)
    }

    override suspend fun getData(name: Name): StageData<T>? = dataSet.getData(name)?.let {
        workspace.internalize(it, name, stageName, stageMeta)
    }
}