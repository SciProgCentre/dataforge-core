package space.kscience.dataforge.names

import space.kscience.dataforge.misc.DFExperimental
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(DFExperimental::class)
class NameMatchTest {
    @Test
    fun matchWildCards() {
        val theName = "a.b.c.d".toName()
        assertTrue { theName.matches("a.b.**") }
        assertTrue { theName.matches("a.*.c.**") }
        assertTrue { theName.matches("**.d") }
        assertTrue { theName.matches("**.b.**") }
        assertTrue { theName.matches("a.*.*.d") }
        assertFails { theName.matches("a.**.d") }
        assertFalse { theName.matches("a.b.c.d.**") }
    }

    @Test
    fun matchPattern() {
        val theName = "a[dd+2].b[13].c.d[\"d\"]".toName()
        assertTrue { theName.matches("a[.*].b[.*].c[.*].d[.*]") }
        assertTrue { theName.matches("a[.*].b[.*].c.d[.*]") }
        assertFalse { theName.matches("a[.*].b[.*].*.d") }
        assertTrue { theName.matches("""\\w[dd\\+2].b[.*].c[.*].d[.*]""") }
        assertFalse { theName.matches("""\\s[dd\\+2].b[.*].c[.*].d[.*]""") }
    }
}