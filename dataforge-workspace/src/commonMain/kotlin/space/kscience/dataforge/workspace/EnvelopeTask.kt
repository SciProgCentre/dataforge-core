package space.kscience.dataforge.workspace

import space.kscience.dataforge.data.DataTree.Companion.META_ITEM_NAME_TOKEN
import space.kscience.dataforge.io.Envelope
import space.kscience.dataforge.io.IOReader
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import kotlin.reflect.KType

public abstract class EnvelopeTask<T : Any>(
    override val descriptor: MetaDescriptor?,
    private val reader: IOReader<T>,
) : Task<T> {

    public abstract suspend fun produceEnvelopes(
        workspace: Workspace,
        taskName: Name,
        taskMeta: Meta,
    ): Map<Name, Envelope>

    override suspend fun execute(workspace: Workspace, taskName: Name, taskMeta: Meta): TaskResult<T> =
        Result(workspace, taskName, taskMeta, reader, produceEnvelopes(workspace, taskName, taskMeta))

    private class Result<T : Any>(
        override val workspace: Workspace,
        override val taskName: Name,
        override val taskMeta: Meta,
        val reader: IOReader<T>,
        envelopes: Map<Name, Envelope>,
    ) : TaskResult<T> {

        private val dataMap = envelopes.mapValues {
            workspace.wrapData(it.value.toData(reader), it.key, taskName, taskMeta)
        }
        override val meta: Meta get() = dataMap[META_ITEM_NAME_TOKEN.asName()]?.meta ?: Meta.EMPTY

        override val dataType: KType get() = reader.type

        override fun iterator(): Iterator<TaskData<T>> = dataMap.values.iterator()

        override fun get(name: Name): TaskData<T>? = dataMap[name]
    }
}

