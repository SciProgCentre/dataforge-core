package hep.dataforge.io

import hep.dataforge.meta.*
import hep.dataforge.values.*
import kotlinx.io.core.Input
import kotlinx.io.core.Output
import kotlinx.io.core.readText
import kotlinx.io.core.writeText

object BinaryMetaFormat : MetaFormat {
    override val name: String = "bin"
    override val key: Short = 0x4249//BI

    override fun Input.readObject(): Meta {
        return (readMetaItem() as MetaItem.NodeItem).node
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

    override fun Output.writeObject(meta: Meta) {
        writeChar('M')
        writeInt(meta.items.size)
        meta.items.forEach { (key, item) ->
            writeString(key.toString())
            when (item) {
                is MetaItem.ValueItem -> {
                    writeValue(item.value)
                }
                is MetaItem.NodeItem -> {
                    writeObject(item.node)
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
                        setItem(name, item)
                    }
                }
                MetaItem.NodeItem(meta)
            }
            else -> error("Unknown serialization key character: $keyChar")
        }
    }
}