package space.kscience.dataforge.meta

import kotlinx.serialization.json.Json
import space.kscience.dataforge.names.asName
import kotlin.test.Test
import kotlin.test.assertEquals

class MetaSerializationTest {

    @Test
    fun singleValueDeserialization() {
        val string = "ddd"
        val meta = Json.decodeFromString(MetaSerializer, string)
        assertEquals(string, meta.value?.string)
    }

    @Test
    fun complexMeta() {
        val meta = Meta {
            "a" put 28.3
            "b" put doubleArrayOf(1.0, 2.0, 3.2)
            "child" put Meta {
                "a" put "aString"
                "sns[0]" put Meta {
                    "d" put 0
                }
                "sns[1]" put Meta {
                    "d" put 1
                }
                setIndexed(
                    "sns2".asName(),
                    listOf(
                        Meta { "d" put "first" },
                        Meta("53")
                    )
                )
            }
        }

        val string = Json.encodeToString(MetaSerializer, meta)
        println(string)
        val reconstructed = Json.decodeFromString(MetaSerializer, string)
        assertEquals(meta, reconstructed)
    }
}