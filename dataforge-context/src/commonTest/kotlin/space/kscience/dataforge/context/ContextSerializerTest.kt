package space.kscience.dataforge.context

import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test
import kotlin.test.assertEquals
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

    class PluginBFail : AbstractPlugin() {
        override val tag: PluginTag = PluginTag("B2")

        override val serializerModule: SerializersModule = SerializersModule {
            polymorphic(Body::class) {
                subclass(CustomBSerializer)
            }
        }

    }

    val polymorphicSerializer = PolymorphicSerializer(Body::class)


    @Test
    fun testContextWithSerializers() {
        val context = Context {
            plugin(PluginA())
            plugin(PluginB())
        }


        val string = context.json.encodeToString(polymorphicSerializer, BodyA())

        assertTrue { context.json.decodeFromString(polymorphicSerializer, string) is BodyA }

    }

    @Test
    fun testContextInheritance() {
        val parentContext = Context {
            plugin(PluginA())
            plugin(PluginBFail())
        }

        val childContext = parentContext.buildContext {
            plugin(PluginB())
        }

        val stringA = childContext.json.encodeToString(polymorphicSerializer, BodyA())

        assertTrue { childContext.json.decodeFromString<Body>(polymorphicSerializer, stringA) is BodyA }
        val stringB = childContext.json.encodeToString(polymorphicSerializer, BodyB())

        assertTrue { childContext.json.decodeFromString<Body>(polymorphicSerializer, stringB) is BodyB }

        assertEquals("\"Fail\"", parentContext.json.encodeToString(polymorphicSerializer, BodyB()))

    }


    @Test
    fun testConflict() {
        val context = Context {
            plugin(PluginA())
            plugin(PluginB())
            plugin(PluginBFail())
        }

        assertFails {
            context.json.encodeToString(polymorphicSerializer, BodyB())
        }
    }

}