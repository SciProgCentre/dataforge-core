package space.kscience.dataforge.meta

import org.junit.jupiter.api.Test
import kotlin.test.assertFails

class JvmMutableMetaTest {
    @Test
    fun recursiveMeta(){
        val meta = MutableMeta {
            "a" put 2
        }

        assertFails { meta["child.a"] = meta }
    }
}