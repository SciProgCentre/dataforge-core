package space.kscience.dataforge.io.yaml

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.io.writeString
import net.mamoe.yamlkt.*
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.io.MetaFormat
import space.kscience.dataforge.io.MetaFormatFactory
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.get
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.withIndex
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

public fun Meta.toYaml(): YamlMap {
    val map: Map<String, Any?> = items.entries.associate { (key, item) ->
        key.toString() to if (item.isLeaf) {
            item.value?.value
        } else {
            item.toYaml()
        }
    }

    return YamlMap(map)
}

private class YamlMeta(private val yamlMap: YamlMap, private val descriptor: MetaDescriptor? = null) : Meta {

    override val value: Value?
        get() = yamlMap.getStringOrNull(null)?.parseValue()

    private fun buildItems(): Map<NameToken, Meta> {
        val map = LinkedHashMap<NameToken, Meta>()

        yamlMap.content.entries.forEach { (key, value) ->
            val stringKey = key.toString()
            val itemDescriptor = descriptor?.get(stringKey)
            val token = NameToken(stringKey)
            when (value) {
                YamlNull -> Meta(Null)
                is YamlLiteral -> map[token] = Meta(value.content.parseValue())
                is YamlMap -> map[token] = value.toMeta()
                is YamlList -> if (value.all { it is YamlLiteral }) {
                    val listValue = ListValue(
                        value.map {
                            //We already checked that all values are primitives
                            (it as YamlLiteral).content.parseValue()
                        }
                    )
                    map[token] = Meta(listValue)
                } else value.forEachIndexed { index, yamlElement ->
                    val indexKey = itemDescriptor?.indexKey
                    val indexValue: String = (yamlElement as? YamlMap)?.getStringOrNull(indexKey)
                        ?: index.toString() //In case index is non-string, the backward transformation will be broken.

                    val tokenWithIndex = token.withIndex(indexValue)
                    map[tokenWithIndex] = yamlElement.toMeta(itemDescriptor)
                }
            }
        }
        return map
    }

    override val items: Map<NameToken, Meta> get() = buildItems()

    override fun toString(): String = Meta.toString(this)
    override fun equals(other: Any?): Boolean = Meta.equals(this, other as? Meta)
    override fun hashCode(): Int = Meta.hashCode(this)
}

public fun YamlElement.toMeta(descriptor: MetaDescriptor? = null): Meta = when (this) {
    YamlNull -> Meta(Null)
    is YamlLiteral -> Meta(content.parseValue())
    is YamlMap -> toMeta()
    //We can't return multiple items therefore we create top level node
    is YamlList -> YamlMap(mapOf("@yamlArray" to this)).toMeta(descriptor)
}

public fun YamlMap.toMeta(): Meta = YamlMeta(this)


/**
 * Represent meta as Yaml
 */
public class YamlMetaFormat(private val meta: Meta) : MetaFormat {

    override fun writeMeta(sink: Sink, meta: Meta, descriptor: MetaDescriptor?) {
        val yaml: YamlMap = meta.toYaml()
        val string = Yaml.encodeToString(YamlMap.serializer(), yaml)
        sink.writeString(string)
    }

    override fun readMeta(source: Source, descriptor: MetaDescriptor?): Meta {
        val yaml = Yaml.decodeYamlMapFromString(source.readString())
        return yaml.toMeta()
    }

    public companion object : MetaFormatFactory {
        override fun build(context: Context, meta: Meta): MetaFormat = YamlMetaFormat(meta)

        override val shortName: String = "yaml"

        override val key: Short = 0x594d //YM

        private val default = YamlMetaFormat(Meta.EMPTY)

        override fun writeMeta(sink: Sink, meta: Meta, descriptor: MetaDescriptor?): Unit =
            default.writeMeta(sink, meta, descriptor)

        override fun readMeta(source: Source, descriptor: MetaDescriptor?): Meta =
            default.readMeta(source, descriptor)
    }
}