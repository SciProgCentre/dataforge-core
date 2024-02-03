package space.kscience.dataforge.workspace

public interface WorkspaceCache {
    public suspend fun <T> cache(result: TaskResult<T>): TaskResult<T>
}
