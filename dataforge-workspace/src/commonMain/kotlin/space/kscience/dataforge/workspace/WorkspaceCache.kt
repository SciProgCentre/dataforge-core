package space.kscience.dataforge.workspace

public interface WorkspaceCache {
    public suspend fun <T : Any> evaluate(result: TaskResult<T>): TaskResult<T>
}
