package hep.dataforge.workspace

import hep.dataforge.meta.Meta
import hep.dataforge.meta.descriptors.Described
import hep.dataforge.misc.Type
import hep.dataforge.workspace.WorkStage.Companion.TYPE

@Type(TYPE)
public interface WorkStage<out R : Any> : Described {

    /**
     * Compute a [StageDataSet] using given meta. In general, the result is lazy and represents both computation model
     * and a handler for actual result
     *
     * @param workspace a workspace to run task model in
     * @param meta configuration for current stage computation
     */
    public suspend fun execute(workspace: Workspace, meta: Meta): StageDataSet<R>

    public companion object {
        public const val TYPE: String = "workspace.stage"
    }
}