package space.kscience.dataforge.properties


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.misc.DFExperimental

@DFExperimental
public fun <T> ObservableMeta.asFlow(converter: MetaSpec<T>): Flow<T> = callbackFlow {
    onChange(this){
        trySend(converter.read(this))
    }

    awaitClose{
        removeListener(this)
    }
}

@DFExperimental
public fun <T> MutableMeta.listenTo(
    scope: CoroutineScope,
    converter: MetaConverter<T>,
    flow: Flow<T>,
): Job = flow.onEach {
    update(converter.convert(it))
}.launchIn(scope)

@DFExperimental
public fun <T> ObservableMutableMeta.bind(
    scope: CoroutineScope,
    converter: MetaConverter<T>,
    flow: MutableSharedFlow<T>,
): Job = scope.launch{
    listenTo(this, converter,flow)
    onChange(flow){
        launch {
            flow.emit(converter.read(this@onChange))
        }
    }
    flow.onCompletion {
        removeListener(flow)
    }
}.also {
    it.invokeOnCompletion {
        removeListener(flow)
    }
}
