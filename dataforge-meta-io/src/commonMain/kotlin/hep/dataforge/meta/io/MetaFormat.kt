package hep.dataforge.meta.io

import hep.dataforge.meta.*
import kotlinx.io.core.Input
import kotlinx.io.core.Output
import kotlinx.io.core.readText
import kotlinx.io.core.writeText

/**
 * A format for meta serialization
 */
interface MetaFormat {
    val name: String
    val key: Short

    suspend fun write(meta: Meta, out: Output)
    suspend fun read(input: Input): Meta
}

///**
// * Resolve format by its name. Null if not provided
// */
//expect fun resolveFormat(name: String): MetaFormat?
//
///**
// * Resolve format by its binary key. Null if not provided
// */
//expect fun resolveFormat(key: Short): MetaFormat?

internal expect fun writeJson(meta: Meta, out: Output)
internal expect fun readJson(input: Input, length: Int = -1): Meta

object JSONMetaFormat : MetaFormat {
    override val name: String = "json"
    override val key: Short = 0x4a53//"JS"

    override suspend fun write(meta: Meta, out: Output) = writeJson(meta, out)
    override suspend fun read(input: Input): Meta = readJson(input)
}

object BinaryMetaFormat : MetaFormat {
    override val name: String = "bin"
    override val key: Short = 0x4249//BI

    override suspend fun write(meta: Meta, out: Output) {
        out.writeMeta(meta)
    }

    override suspend fun read(input: Input): Meta {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun Output.writeChar(char: Char) = writeByte(char.toByte())

    private fun Output.writeString(str: String) {
        writeInt(str.length)
        writeText(str)
    }

    private fun Output.writeValue(value: Value) {
        if (value.isList()) {
            writeChar('L')
            writeInt(value.list.size)
            value.list.forEach {
                writeValue(it)
            }
        } else when (value.type) {
            ValueType.NUMBER -> when (value.value) {
                is Short -> {
                    writeChar('s')
                    writeShort(value.number.toShort())
                }
                is Int -> {
                    writeChar('i')
                    writeInt(value.number.toInt())
                }
                is Long -> {
                    writeChar('l')
                    writeLong(value.number.toLong())
                }
                is Float -> {
                    writeChar('f')
                    writeFloat(value.number.toFloat())
                }
                else -> {
                    writeChar('d')
                    writeDouble(value.number.toDouble())
                }
            }
            ValueType.STRING -> {
                writeChar('S')
                writeString(value.string)
            }
            ValueType.BOOLEAN -> {
                if (value.boolean) {
                    writeChar('+')
                } else {
                    writeChar('-')
                }
            }
            ValueType.NULL -> {
                writeChar('N')
            }
        }
    }

    private fun Output.writeMeta(meta: Meta) {
        writeChar('M')
        writeInt(meta.items.size)
        meta.items.forEach { (key, item) ->
            writeString(key)
            when (item) {
                is MetaItem.ValueItem -> {
                    writeValue(item.value)
                }
                is MetaItem.SingleNodeItem -> {
                    writeMeta(item.node)
                }
                is MetaItem.MultiNodeItem -> {
                    writeChar('#')
                    writeInt(item.nodes.size)
                    item.nodes.forEach {
                        writeMeta(it)
                    }
                }
            }
        }
    }

    private fun Input.readString(): String {
        val length = readInt()
        return readText(max = length)
    }

    private fun Input.readMetaItem(): MetaItem<MetaBuilder> {
        val keyChar = readByte().toChar()
        return when (keyChar) {
            'S' -> MetaItem.ValueItem(StringValue(readString()))
            'N' -> MetaItem.ValueItem(Null)
            '+' -> MetaItem.ValueItem(True)
            '-' -> MetaItem.ValueItem(True)
            's' -> MetaItem.ValueItem(NumberValue(readShort()))
            'i' -> MetaItem.ValueItem(NumberValue(readInt()))
            'l' -> MetaItem.ValueItem(NumberValue(readInt()))
            'f' -> MetaItem.ValueItem(NumberValue(readFloat()))
            'd' -> MetaItem.ValueItem(NumberValue(readDouble()))
            'L' -> {
                val length = readInt()
                val list = (1..length).map { (readMetaItem() as MetaItem.ValueItem).value }
                MetaItem.ValueItem(Value.of(list))
            }
            'M' -> {
                val length = readInt()
                val meta = buildMeta {
                    (1..length).forEach { _ ->
                        val name = readString()
                        val item = readMetaItem()
                        set(name,item)
                    }
                }
                MetaItem.SingleNodeItem(meta)
            }
            '#' -> {
                val length = readInt()
                val nodes = (1..length).map { (readMetaItem() as MetaItem.SingleNodeItem).node }
                MetaItem.MultiNodeItem(nodes)
            }
            else -> error("Unknown serialization key character: $keyChar")
        }
    }

}

