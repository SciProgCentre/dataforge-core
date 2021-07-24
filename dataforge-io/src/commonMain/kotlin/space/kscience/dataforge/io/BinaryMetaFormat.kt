package space.kscience.dataforge.io

import io.ktor.utils.io.core.*
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.meta.descriptors.NodeDescriptor
import space.kscience.dataforge.values.*

/**
 * A DataForge-specific simplified binary format for meta
 * TODO add description
 */
public object BinaryMetaFormat : MetaFormat, MetaFormatFactory {
    override val shortName: String = "bin"
    override val key: Short = 0x4249//BI

    override fun invoke(meta: Meta, context: Context): MetaFormat = this

    override fun readMeta(input: Input, descriptor: NodeDescriptor?): Meta {
        return (input.readMetaItem() as MetaItemNode).node
    }

    private fun Output.writeChar(char: Char) = writeByte(char.code.toByte())

    private fun Output.writeString(str: String) {
        writeInt(str.length)
        writeFully(str.encodeToByteArray())
    }

    public fun Output.writeValue(value: Value): Unit = when (value.type) {
        ValueType.NUMBER -> when (value.value) {
            is Short -> {
                writeChar('s')
                writeShort(value.short)
            }
            is Int -> {
                writeChar('i')
                writeInt(value.int)
            }
            is Long -> {
                writeChar('l')
                writeLong(value.long)
            }
            is Float -> {
                writeChar('f')
                writeFloat(value.float)
            }
            else -> {
                writeChar('d')
                writeDouble(value.double)
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
        ValueType.LIST -> {
            writeChar('L')
            writeInt(value.list.size)
            value.list.forEach {
                writeValue(it)
            }
        }
    }

    override fun writeMeta(
        output: Output,
        meta: Meta,
        descriptor: space.kscience.dataforge.meta.descriptors.NodeDescriptor?,
    ) {
        output.writeChar('M')
        output.writeInt(meta.items.size)
        meta.items.forEach { (key, item) ->
            output.writeString(key.toString())
            when (item) {
                is MetaItemValue -> {
                    output.writeValue(item.value)
                }
                is MetaItemNode -> {
                    writeObject(output, item.node)
                }
            }
        }
    }

    private fun Input.readString(): String {
        val length = readInt()
        val array = readBytes(length)
        return array.decodeToString()
    }

    @Suppress("UNCHECKED_CAST")
    public fun Input.readMetaItem(): TypedMetaItem<MutableMeta> {
        return when (val keyChar = readByte().toInt().toChar()) {
            'S' -> MetaItemValue(StringValue(readString()))
            'N' -> MetaItemValue(Null)
            '+' -> MetaItemValue(True)
            '-' -> MetaItemValue(True)
            's' -> MetaItemValue(NumberValue(readShort()))
            'i' -> MetaItemValue(NumberValue(readInt()))
            'l' -> MetaItemValue(NumberValue(readInt()))
            'f' -> MetaItemValue(NumberValue(readFloat()))
            'd' -> MetaItemValue(NumberValue(readDouble()))
            'L' -> {
                val length = readInt()
                val list = (1..length).map { (readMetaItem() as MetaItemValue).value }
                MetaItemValue(Value.of(list))
            }
            'M' -> {
                val length = readInt()
                val meta = Meta {
                    (1..length).forEach { _ ->
                        val name = readString()
                        val item = readMetaItem()
                        set(name, item)
                    }
                }
                MetaItemNode(meta)
            }
            else -> error("Unknown serialization key character: $keyChar")
        }
    }
}