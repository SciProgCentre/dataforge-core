package hep.dataforge.io

import hep.dataforge.context.Context
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.MetaItem
import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.meta.setItem
import hep.dataforge.values.*
import kotlinx.io.*
import kotlinx.io.text.readUtf8String
import kotlinx.io.text.writeUtf8String

/**
 * A DataForge-specific simplified binary format for meta
 * TODO add description
 */
public object BinaryMetaFormat : MetaFormat, MetaFormatFactory {
    override val shortName: String = "bin"
    override val key: Short = 0x4249//BI

    override fun invoke(meta: Meta, context: Context): MetaFormat = this

    override fun readMeta(input: Input, descriptor: NodeDescriptor?): Meta {
        return (input.readMetaItem() as MetaItem.NodeItem).node
    }

    private fun Output.writeChar(char: Char) = writeByte(char.toByte())

    private fun Output.writeString(str: String) {
        writeInt(str.length)
        writeUtf8String(str)
    }

    public fun Output.writeValue(value: Value) {
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

    override fun writeMeta(
        output: kotlinx.io.Output,
        meta: hep.dataforge.meta.Meta,
        descriptor: hep.dataforge.meta.descriptors.NodeDescriptor?
    ) {
        output.writeChar('M')
        output.writeInt(meta.items.size)
        meta.items.forEach { (key, item) ->
            output.writeString(key.toString())
            when (item) {
                is MetaItem.ValueItem -> {
                    output.writeValue(item.value)
                }
                is MetaItem.NodeItem -> {
                    writeObject(output, item.node)
                }
            }
        }
    }

    private fun Input.readString(): String {
        val length = readInt()
        return readUtf8String(length)
    }

    @Suppress("UNCHECKED_CAST")
    public fun Input.readMetaItem(): MetaItem<MetaBuilder> {
        return when (val keyChar = readByte().toChar()) {
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
                val meta = Meta {
                    (1..length).forEach { _ ->
                        val name = readString()
                        val item = readMetaItem()
                        setItem(name, item)
                    }
                }
                MetaItem.NodeItem(meta)
            }
            else -> error("Unknown serialization key character: $keyChar")
        }
    }
}