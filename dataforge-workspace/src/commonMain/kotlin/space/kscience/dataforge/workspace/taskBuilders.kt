package space.kscience.dataforge.workspace

import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.meta.Meta

public suspend fun <T : Any> TaskResultBuilder<*>.from(
    selector: DataSelector<T>,
    meta: Meta = Meta.EMPTY,
): DataSet<T> = selector.select(workspace, meta)
