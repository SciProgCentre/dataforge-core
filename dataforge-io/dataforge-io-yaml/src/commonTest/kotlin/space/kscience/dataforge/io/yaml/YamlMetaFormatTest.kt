package space.kscience.dataforge.io.yaml

import kotlinx.io.Buffer
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.Name
import kotlin.test.Test
import kotlin.test.assertEquals

class YamlMetaFormatTest {

    private fun assertRoundTrip(meta: Meta) {
        // Direct YamlNode round-trip
        val yamlNode = meta.toYamlNode()
        val restoredFromNode = yamlNode.toMeta()
        assertEquals(meta, restoredFromNode, "Failed node round-trip for meta: $meta")

        // Stream (Sink/Source) round-trip via KotamlMetaFormat
        val buffer = Buffer()
        YamlMetaFormat.writeMeta(buffer, meta)
        val restoredFromStream = YamlMetaFormat.readMeta(buffer)
        assertEquals(meta, restoredFromStream, "Failed stream round-trip for meta: $meta")
    }

    @Test
    fun testEmptyMeta() {
        assertRoundTrip(Meta.EMPTY)
    }

    @Test
    fun testScalarMeta() {
        assertRoundTrip(Meta { value = "hello".asValue() })
        assertRoundTrip(Meta { value = 123.asValue() })
        assertRoundTrip(Meta { value = true.asValue() })
        assertRoundTrip(Meta { value = 3.14.asValue() })
        assertRoundTrip(Meta { value = Null })
    }

    @Test
    fun testSimpleMapMeta() {
        val meta = Meta {
            "title" put "Test Title"
            "count" put 42
            "active" put true
            "score" put 99.5
        }
        assertRoundTrip(meta)
    }

    @Test
    fun testNestedMapMeta() {
        val meta = Meta {
            "database" put Meta {
                "host" put "localhost"
                "port" put 5432
                "credentials" put Meta {
                    "user" put "admin"
                    "password" put "secret"
                }
            }
            "version" put 2
        }
        assertRoundTrip(meta)
    }

    @Test
    fun testMetaWithValueAndChildren() {
        val meta = Meta {
            value = "root-value".asValue()
            "child1" put "value1"
            "child2" put Meta {
                value = "nested-root".asValue()
                "subchild" put 100
            }
        }
        assertRoundTrip(meta)
    }

    @Test
    fun testScalarListMeta() {
        val meta = Meta {
            "tags" put listOf("kotlin".asValue(), "snark".asValue(), "yaml".asValue()).asValue()
            "numbers" put listOf(1.asValue(), 2.asValue(), 3.asValue(), 4.asValue()).asValue()
            "flags" put listOf(true.asValue(), false.asValue(), true.asValue()).asValue()
        }
        assertRoundTrip(meta)
    }

    @Test
    fun testTopLevelScalarListMeta() {
        val meta = Meta {
            value = listOf("item1".asValue(), "item2".asValue(), "item3".asValue()).asValue()
        }
        assertRoundTrip(meta)
    }

    @Test
    fun testListOfMetaNodes() {
        val meta = Meta {
            setIndexed(
                Name.of("items"),
                listOf(
                    Meta {
                        "id" put 1
                        "name" put "first"
                    },
                    Meta {
                        "id" put 2
                        "name" put "second"
                    }
                )
            )
        }
        assertRoundTrip(meta)
    }

    @Test
    fun testSpecialCharactersAndMultilineStrings() {
        val meta = Meta {
            "url" put "https://sciprog.center/about?lang=en&theme=dark"
            "description" put "Line 1\nLine 2\nLine 3"
            "quoted" put "Text with \"double\" and 'single' quotes"
            "unicode" put "Kotlin / 科学 / Наука / \uD83D\uDE80"
            "colonInValue" put "key: value"
            "yamlSymbols" put "# not a comment, [not an array], {not an object}"
        }
        assertRoundTrip(meta)
    }

    @Test
    fun testNumericTypesAndEdgeCases() {
        val meta = Meta {
            "zero" put 0
            "negativeInt" put -42
            "positiveInt" put 1000000
            "negativeFloat" put -123.456
            "zeroFloat" put 0.0
            "positiveFloat" put 3.14159
        }
        assertRoundTrip(meta)
    }

    @Test
    fun testDeeplyNestedStructure() {
        val meta = Meta {
            "level1" put Meta {
                "level2" put Meta {
                    "level3" put Meta {
                        "level4" put Meta {
                            "level5" put Meta {
                                "leaf" put "deep"
                            }
                        }
                    }
                }
            }
        }
        assertRoundTrip(meta)
    }

    @Test
    fun testFormatFactoryProperties() {
        assertEquals("yaml", YamlMetaFormat.shortName)
        assertEquals(0x594d.toShort(), YamlMetaFormat.key)
    }

    @Test
    fun testComplexDocumentMeta() {
        val meta = Meta {
            "title" put "Snark Site Generation"
            "author" put Meta {
                "name" put "Alexander"
                "email" put "altavir@gmail.com"
            }
            "keywords" put listOf("kscience".asValue(), "dataforge".asValue(), "snark".asValue()).asValue()
            "config" put Meta {
                "debug" put true
                "maxRetries" put 3
                "timeoutMs" put 15000
            }
            setIndexed(
                Name.of("sections"),
                listOf(
                    Meta {
                        "title" put "Introduction"
                        "order" put 1
                    },
                    Meta {
                        "title" put "Architecture"
                        "order" put 2
                    }
                )
            )
        }
        assertRoundTrip(meta)
    }
}
