package space.kscience.dataforge.io.yaml

import com.charleskorn.kaml.*
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.okio.asOkioSink
import kotlinx.io.readByteString
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.io.MetaFormat
import space.kscience.dataforge.io.MetaFormatFactory
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.descriptors.get
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.asName

private val SYNTHETIC_LOCATION = Location(1, 1)

private fun Value.toYamlNode(path: YamlPath): YamlNode = when {
    isList() -> YamlList(list.mapIndexed { index, listValue ->
        listValue.toYamlNode(
            path.withListEntry(index, SYNTHETIC_LOCATION)
        )
    }, path)

    else -> YamlScalar(string, path)

}

/**
 * Converts a [Meta] object into a [YamlNode] representation.
 * The resulting [YamlNode] can either be a [YamlMap], [YamlList], [YamlScalar], or [YamlNull],
 * depending on the structure and content of the [Meta] object.
 *
 * @param path The initial [YamlPath] for the node. Defaults to the root path.
 * @return A [YamlNode] representation of the current [Meta] object.
 */
public fun Meta.toYamlNode(path: YamlPath = YamlPath.root): YamlNode {
    if (items.isNotEmpty()) {
        val map: Map<YamlScalar, YamlNode> = buildMap {
            items.entries.forEach { (token, child) ->
                val keyPath = path.withMapElementKey(token.toString(), SYNTHETIC_LOCATION)
                val key = YamlScalar(token.toString(), keyPath, plain = false)
                val value = child.toYamlNode(keyPath.withMapElementValue(SYNTHETIC_LOCATION))
                put(key, value)
            }
            value?.let {
                val keyPath = path.withMapElementKey(Meta.VALUE_KEY, SYNTHETIC_LOCATION)
                put(YamlScalar(Meta.VALUE_KEY, keyPath), it.toYamlNode(path.withMapElementValue(SYNTHETIC_LOCATION)))
            }
        }
        return YamlMap(map, path)
    } else {
        val value = value
        return when {
            value == null -> YamlNull(path)
            value.isList() -> YamlList(value.list.mapIndexed { index, listValue ->
                listValue.toYamlNode(
                    path.withListEntry(index, SYNTHETIC_LOCATION)
                )
            }, path)

            else -> YamlScalar(value.string, path)

        }
    }

}


private fun YamlNode.toValueOrNull(): Value? = when (this) {
    is YamlList -> items.mapNotNull { it.toValueOrNull() }.asValue()
    is YamlMap -> null
    is YamlNull -> Null
    is YamlScalar -> Value.parse(content)
    is YamlTaggedNode -> null
}

private fun MutableMeta.setYaml(token: NameToken?, yaml: YamlNode, descriptor: MetaDescriptor?) {
    if (token?.toString() == Meta.VALUE_KEY) {
        this.value = yaml.toValueOrNull()
        return
    }
    when (yaml) {
        is YamlScalar -> {
            val value = Value.parse(yaml.content)
            if (token == null) {
                this.value = value
            } else {
                setValue(token.asName(), value)
            }
        }

        is YamlMap -> if (token == null) {
            yaml.entries.forEach { (key, node) ->
                val token = NameToken.parse(key.content)
                setYaml(token, node, descriptor?.get(token.body))
            }
        } else {
            set(
                token,
                Meta {
                    yaml.entries.forEach { (key, node) ->
                        val token = NameToken.parse(key.content)
                        setYaml(token, node, descriptor?.get(token.body))
                    }
                }
            )

        }

        is YamlList -> if (yaml.items.all { it is YamlNull || it is YamlScalar }) {
            if (token == null) {
                value = yaml.toValueOrNull()
            } else {
                setValue(token.asName(), yaml.toValueOrNull())
            }
        } else {
            val name = token?.asName() ?: Name.of("@jsonArray")
            setIndexed(name, yaml.items.map { it.toMeta(token?.body?.let { tokenBody -> descriptor?.get(tokenBody) }) })
        }

        else -> {
            // do nothing
        }
    }
}

/**
 * Converts the current [YamlNode] instance into a [Meta] representation.
 *
 * @param descriptor An optional [MetaDescriptor] that provides structural information
 * about the meta node to be created, such as allowed child nodes, allowed value types,
 * and other constraints. If null, no descriptor constraints will be used.
 * @return A [Meta] object created based on the structure and content of the [YamlNode].
 */
public fun YamlNode.toMeta(descriptor: MetaDescriptor? = null): Meta = Meta {
    setYaml(null, this@toMeta, descriptor)
}

/**
 * Represent meta as Yaml
 */
public class YamlMetaFormat(private val meta: Meta) : MetaFormat {

    override fun writeMeta(sink: Sink, meta: Meta, descriptor: MetaDescriptor?) {
        val yaml: YamlNode = meta.toYamlNode()
        Yaml.default.encodeToSink(YamlNode.serializer(), yaml, sink.asOkioSink())
    }

    override fun readMeta(source: Source, descriptor: MetaDescriptor?): Meta {
        val yaml = Yaml.default.parseToYamlNode(source.readByteString().decodeToString())
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