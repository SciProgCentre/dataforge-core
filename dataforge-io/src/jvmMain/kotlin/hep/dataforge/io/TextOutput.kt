package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.meta.Meta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class TextOutput(override val context: Context, private val output: kotlinx.io.core.Output) : Output<Any> {
    override fun render(obj: Any, meta: Meta) {
        context.launch(Dispatchers.IO) {
            output.append(obj.toString())
            output.append('\n')
        }
    }
}