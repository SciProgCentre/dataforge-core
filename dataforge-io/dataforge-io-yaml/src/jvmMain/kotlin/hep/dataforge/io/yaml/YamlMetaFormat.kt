package hep.dataforge.io.yaml

import hep.dataforge.context.Context
import hep.dataforge.io.IOFormat.Companion.META_KEY
import hep.dataforge.io.IOFormat.Companion.NAME_KEY
import hep.dataforge.io.MetaFormat
import hep.dataforge.io.MetaFormatFactory
import hep.dataforge.meta.*
import hep.dataforge.meta.descriptors.ItemDescriptor
import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.names.NameToken
import hep.dataforge.names.withIndex
import hep.dataforge.values.ListValue
import hep.dataforge.values.Null
import hep.dataforge.values.parseValue
import kotlinx.io.Input
import kotlinx.io.Output
import kotlinx.io.text.readUtf8String
import kotlinx.io.text.writeUtf8String
import net.mamoe.yamlkt.*

public fun Meta.toYaml(): YamlMap {
    val map: Map<String, Any?> = items.entries.associate { (key, item) ->
        key.toString() to when (item) {
            is MetaItemValue -> {
                item.value.value
            }
            is MetaItemNode -> {
                item.node.toYaml()
            }
        }
    }
    return YamlMap(map)
}

private class YamlMeta(private val yamlMap: YamlMap, private val descriptor: NodeDescriptor? = null) : MetaBase() {

    private fun buildItems(): Map<NameToken, MetaItem> {
        val map = LinkedHashMap<NameToken, MetaItem>()

        yamlMap.content.entries.forEach { (key, value) ->
            val stringKey = key.toString()
            val itemDescriptor = descriptor?.items?.get(stringKey)
            val token = NameToken(stringKey)
            when (value) {
                YamlNull -> Null.asMetaItem()
                is YamlLiteral -> map[token] = value.content.parseValue().asMetaItem()
                is YamlMap -> map[token] = value.toMeta().asMetaItem()
                is YamlList -> if (value.all { it is YamlLiteral }) {
                    val listValue = ListValue(
                        value.map {
                            //We already checked that all values are primitives
                            (it as YamlLiteral).content.parseValue()
                        }
                    )
                    map[token] = MetaItemValue(listValue)
                } else value.forEachIndexed { index, yamlElement ->
                    val indexKey = (itemDescriptor as? NodeDescriptor)?.indexKey ?: ItemDescriptor.DEFAULT_INDEX_KEY
                    val indexValue: String = (yamlElement as? YamlMap)?.getStringOrNull(indexKey)
                        ?: index.toString() //In case index is non-string, the backward transformation will be broken.

                    val tokenWithIndex = token.withIndex(indexValue)
                    map[tokenWithIndex] = yamlElement.toMetaItem(itemDescriptor)
                }
            }
        }
        return map
    }

    override val items: Map<NameToken, MetaItem> get() = buildItems()
}

public fun YamlElement.toMetaItem(descriptor: ItemDescriptor? = null): MetaItem = when (this) {
    YamlNull -> Null.asMetaItem()
    is YamlLiteral -> content.parseValue().asMetaItem()
    is YamlMap -> toMeta().asMetaItem()
    //We can't return multiple items therefore we create top level node
    is YamlList -> YamlMap(mapOf("@yamlArray" to this)).toMetaItem(descriptor)
}

public fun YamlMap.toMeta(): Meta = YamlMeta(this)


/**
 * Represent meta as Yaml
 */
@DFExperimental
public class YamlMetaFormat(private val meta: Meta) : MetaFormat {
    private val coder = Yaml.default

    override fun writeMeta(output: Output, meta: Meta, descriptor: NodeDescriptor?) {
        val yaml = meta.toYaml()
        val string = coder.encodeToString(yaml)
        output.writeUtf8String(string)
    }

    override fun readMeta(input: Input, descriptor: NodeDescriptor?): Meta {
        val yaml = coder.decodeYamlMapFromString(input.readUtf8String())
        return yaml.toMeta()
    }

    override fun toMeta(): Meta = Meta {
        NAME_KEY put FrontMatterEnvelopeFormat.name.toString()
        META_KEY put meta
    }

    public companion object : MetaFormatFactory {
        override fun invoke(meta: Meta, context: Context): MetaFormat = YamlMetaFormat(meta)

        override val shortName: String = "yaml"

        override val key: Short = 0x594d //YM

        private val default = YamlMetaFormat()

        override fun writeMeta(output: Output, meta: Meta, descriptor: NodeDescriptor?): Unit =
            default.writeMeta(output, meta, descriptor)

        override fun readMeta(input: kotlinx.io.Input, descriptor: NodeDescriptor?): Meta =
            default.readMeta(input, descriptor)
    }
}