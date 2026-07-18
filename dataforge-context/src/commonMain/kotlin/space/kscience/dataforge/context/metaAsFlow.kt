package space.kscience.dataforge.context


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.misc.DFExperimental

/**
 * Converts an [ObservableMeta] to a cold [Flow] that emits values of type [T] whenever the [ObservableMeta] changes.
 * The provided [converter] is used for transforming the meta state into the target type [T].
 *
 * @param T The type of the values emitted by the resulting [Flow].
 * @param converter The [MetaReader] used to convert the state of the [ObservableMeta] to values of type [T].
 * @return A cold [Flow] emitting values of type [T] whenever the observed meta changes.
 */
@DFExperimental
public fun <T> ObservableMeta.asFlow(converter: MetaReader<T>): Flow<T> = callbackFlow {
    onChange(this) {
        trySend(converter.read(this))
    }

    awaitClose {
        removeListener(this)
    }
}

/**
 * Listens to changes in the [MutableMeta] and updates it with values from the provided [Flow].
 *
 * @param scope The coroutine scope in which the listener is launched.
 * @param converter The [MetaConverter] used to convert values from the [Flow] to the [MutableMeta] state.
 * @param flow The [Flow] from which values are read and used to update the [MutableMeta].
 * @return A [Job] representing the launched coroutine that listens to changes in the [MutableMeta].
 */
@DFExperimental
public fun <T> MutableMeta.listenTo(
    scope: CoroutineScope,
    converter: MetaConverter<T>,
    flow: Flow<T>,
): Job = flow.onEach {
    update(converter.convert(it))
}.launchIn(scope)

/**
 * Binds the [MutableMeta] to a [MutableSharedFlow] of type [T], updating the [MutableMeta] with values from the [MutableSharedFlow].
 *
 * @param scope The coroutine scope in which the listener is launched.
 * @param converter The [MetaConverter] used to convert values from the [MutableSharedFlow] to the [MutableMeta] state.
 * @param flow The [MutableSharedFlow] from which values are read and used to update the [MutableMeta].
 * @return A [Job] representing the launched coroutine that listens to changes in the [MutableMeta].
 */
@DFExperimental
public fun <T> ObservableMutableMeta.bind(
    scope: CoroutineScope,
    converter: MetaConverter<T>,
    flow: MutableSharedFlow<T>,
): Job = scope.launch {
    listenTo(this, converter, flow)
    onChange(flow) {
        launch {
            flow.emit(converter.read(this@onChange))
        }
    }
}.also {
    it.invokeOnCompletion {
        removeListener(flow)
    }
}
