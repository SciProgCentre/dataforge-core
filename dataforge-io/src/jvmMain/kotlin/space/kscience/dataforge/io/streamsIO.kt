package space.kscience.dataforge.io

//
//private class BlockingStreamInput(val source: InputStream) : AbstractInput() {
//    override fun closeSource() {
//        source.close()
//    }
//
//    override fun fill(destination: Memory, offset: Int, length: Int): Int {
//        while (source.available() == 0) {
//            //block until input is available
//        }
//        // Zero-copy attempt
//        if (buffer.buffer.hasArray()) {
//            val result = source.read(buffer.buffer.array(), startIndex, endIndex - startIndex)
//            return result.coerceAtLeast(0) // -1 when IS is closed
//        }
//
//        for (i in startIndex until endIndex) {
//            val byte = source.read()
//            if (byte == -1) return (i - startIndex)
//            buffer[i] = byte.toByte()
//        }
//        return endIndex - startIndex
//    }
//}
//
//public fun <R> InputStream.read(size: Int, block: Input.() -> R): R {
//    val buffer = ByteArray(size)
//    read(buffer)
//    return buffer.asBinary().read(block = block)
//}
//
//public fun <R> InputStream.read(block: Input.() -> R): R = asInput().block()
//
//public fun <R> InputStream.readBlocking(block: Input.() -> R): R = BlockingStreamInput(this).block()
//
//public inline fun OutputStream.write(block: Output.() -> Unit) {
//    asOutput().block()
//}