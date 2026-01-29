package space.kscience.dataforge.context

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer
import space.kscience.dataforge.names.asName
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertTrue

class ContextSerializerTest {
    interface Body

    @Serializable
    class BodyA : Body

    @Serializable
    class BodyB : Body

    object CustomBSerializer : KSerializer<BodyB> {
        private val bSerializer = BodyB.serializer()
        override val descriptor: SerialDescriptor = bSerializer.descriptor

        override fun serialize(encoder: Encoder, value: BodyB) {
            encoder.encodeString("Fail")
        }

        override fun deserialize(decoder: Decoder): BodyB {
            return decoder.decodeSerializableValue(bSerializer)
        }

    }


    class PluginA : AbstractPlugin() {
        override val tag: PluginTag = PluginTag("A")

        override val serializerModule: SerializersModule = SerializersModule {
            polymorphic(Body::class) {
                subclass(BodyA.serializer())
            }
        }

    }

    class PluginB : AbstractPlugin() {
        override val tag: PluginTag = PluginTag("B")

        override val serializerModule: SerializersModule = SerializersModule {
            polymorphic(Body::class) {
                subclass(BodyB.serializer())
            }
        }

    }

    class PluginB2 : AbstractPlugin() {
        override val tag: PluginTag = PluginTag("B2")

        override val serializerModule: SerializersModule = SerializersModule {
            polymorphic(Body::class) {
                subclass(CustomBSerializer)
            }
        }

    }


    @Test
    fun testContextWithSerializers() {
        val context = Context("test") {
            plugin(PluginA())
            plugin(PluginB())
        }


        val string = context.json.encodeToString(serializer<Body>(), BodyA())

        assertTrue { context.json.decodeFromString<Body>(string) is BodyA }

    }

    @Test
    fun testContextInheritance() {
        val parentContext = Context("parent") {
            plugin(PluginA())
        }

        val childContext = parentContext.buildContext("child".asName()) {
            plugin(PluginB())
        }


        val stringA = childContext.json.encodeToString(serializer<Body>(), BodyA())

        assertTrue { childContext.json.decodeFromString<Body>(stringA) is BodyA }
        val stringB = childContext.json.encodeToString(serializer<Body>(), BodyB())

        assertTrue { childContext.json.decodeFromString<Body>(stringB) is BodyB }
    }


    @Test
    fun testConflict() {
        val context = Context("test") {
            plugin(PluginA())
            plugin(PluginB())
            plugin(PluginB2())
        }

        assertFails {
            context.json.encodeToString(serializer<Body>(), BodyB())
        }
    }

}