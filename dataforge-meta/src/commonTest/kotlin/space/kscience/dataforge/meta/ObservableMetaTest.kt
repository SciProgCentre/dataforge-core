package space.kscience.dataforge.meta

import space.kscience.dataforge.names.startsWith
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class ObservableMetaTest {

    @Test
    fun asObservable() {
        val meta = MutableMeta {
            "data" put {
                "x" put ListValue(1, 2, 3)
                "y" put ListValue(5, 6, 7)
                "type" put "scatter"
            }
        }.asObservable()

        println(meta)

        assertEquals("scatter", meta["data.type"].string)
    }

    @Test
    @Ignore
    fun detachNode() {
        val meta = MutableMeta {
            "data" put {
                "x" put ListValue(1, 2, 3)
                "y" put ListValue(5, 6, 7)
                "type" put "scatter"
            }
        }.asObservable()

        var collector: Value? = null

        meta.onChange(null) { name ->
            if (name.startsWith("data")) {
                collector = get("data.z")?.value
            }
        }

        val data = meta["data"]!!

        meta.remove("data")

        data["z"] = ListValue(2, 5, 7)
        assertEquals(null, collector)
    }
}