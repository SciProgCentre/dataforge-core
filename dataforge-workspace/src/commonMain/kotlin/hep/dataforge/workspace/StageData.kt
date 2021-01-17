package hep.dataforge.workspace

import hep.dataforge.data.Data
import hep.dataforge.data.NamedData
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name

/**
 * A [Workspace]-locked [NamedData], that serves as a computation model.
 */
public interface StageData<out T : Any> : NamedData<T> {
    /**
     * The [Workspace] this data belongs to
     */
    public val workspace: Workspace

    /**
     * The name of the stage that produced this data. [Name.EMPTY] if the workspace intrinsic data is used.
     */
    public val stage: Name

    /**
     * Stage configuration used to produce this data.
     */
    public val stageMeta: Meta

    /**
     * Dependencies that allow to compute transitive dependencies as well.
     */
    override val dependencies: Collection<StageData<*>>
}

private class StageDataImpl<out T : Any>(
    override val workspace: Workspace,
    override val data: Data<T>,
    override val name: Name,
    override val stage: Name,
    override val stageMeta: Meta,
) : StageData<T>, Data<T> by data {
    override val dependencies: Collection<StageData<*>> = data.dependencies.map {
        it as? StageData<*> ?: error("StageData can't depend on external data")
    }
}

internal fun <T : Any> Workspace.internalize(data: Data<T>, name: Name, stage: Name, stageMeta: Meta): StageData<T> =
    StageDataImpl(this, data, name, stage, stageMeta)

