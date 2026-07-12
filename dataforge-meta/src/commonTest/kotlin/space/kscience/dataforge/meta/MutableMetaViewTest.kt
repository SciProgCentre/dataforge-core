package space.kscience.dataforge.meta

import space.kscience.dataforge.names.Name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MutableMetaViewTest {
    @Test
    fun metaView() {
        val meta = MutableMeta()
        val view = meta.view(Name.of("a"))

        view["b"] = Meta.EMPTY

        assertTrue { meta.items.isEmpty() }

        view["c"] = Meta {
            "d" put 22
        }

        assertEquals(22, meta["a.c.d"].int)
    }

}