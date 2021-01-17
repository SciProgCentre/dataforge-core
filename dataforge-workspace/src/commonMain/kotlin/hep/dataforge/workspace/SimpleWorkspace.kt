package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.gather
import hep.dataforge.data.DataSet
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name


/**
 * A simple workspace without caching
 */
public class SimpleWorkspace(
    override val context: Context,
    override val data: DataSet<Any>,
    override val targets: Map<String, Meta>,
    stages: Map<Name, WorkStage<Any>>
) : Workspace {

    override val stages: Map<Name, WorkStage<*>> by lazy {
        context.gather<WorkStage<*>>(WorkStage.TYPE) + stages
    }

    public companion object {

    }
}