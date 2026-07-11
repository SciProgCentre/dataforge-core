package space.kscience.dataforge.names

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NamePatternTest {

    @Test
    fun testExactMatch() {
        val name = Name.parse("a.b.c")
        val pattern = NamePattern(listOf(
            NameTokenPattern.Token(StringPattern.Exact("a"), StringPattern.Any),
            NameTokenPattern.Token(StringPattern.Exact("b"), StringPattern.Any),
            NameTokenPattern.Token(StringPattern.Exact("c"), StringPattern.Any)
        ))
        assertTrue(name.matches(pattern))
    }

    @Test
    fun testAnySingleToken() {
        val name = Name.parse("a.b.c")
        val pattern = NamePattern(listOf(
            NameTokenPattern.Token(StringPattern.Exact("a"), StringPattern.Any),
            NameTokenPattern.AnySingleToken,
            NameTokenPattern.Token(StringPattern.Exact("c"), StringPattern.Any)
        ))
        assertTrue(name.matches(pattern))
    }

    @Test
    fun testAnyMultipleTokensAtEnd() {
        val name = Name.parse("a.b.c.d")
        val pattern = NamePattern(listOf(
            NameTokenPattern.Token(StringPattern.Exact("a"), StringPattern.Any),
            NameTokenPattern.Token(StringPattern.Exact("b"), StringPattern.Any),
            NameTokenPattern.AnyMultipleTokens
        ))
        assertTrue(name.matches(pattern))
    }

    @Test
    fun testAnyMultipleTokensAtStart() {
        val name = Name.parse("a.b.c.d")
        val pattern = NamePattern(listOf(
            NameTokenPattern.AnyMultipleTokens,
            NameTokenPattern.Token(StringPattern.Exact("c"), StringPattern.Any),
            NameTokenPattern.Token(StringPattern.Exact("d"), StringPattern.Any)
        ))
        assertTrue(name.matches(pattern))
    }

    @Test
    fun testIndexMatch() {
        val name = Name.parse("a[1].b")
        val pattern = NamePattern(listOf(
            NameTokenPattern.Token(StringPattern.Exact("a"), StringPattern.Exact("1")),
            NameTokenPattern.Token(StringPattern.Exact("b"), StringPattern.Any)
        ))
        assertTrue(name.matches(pattern))
        
        val pattern2 = NamePattern(listOf(
            NameTokenPattern.Token(StringPattern.Exact("a"), StringPattern.Exact("2")),
            NameTokenPattern.Token(StringPattern.Exact("b"), StringPattern.Any)
        ))
        assertFalse(name.matches(pattern2))
    }

    @Test
    fun testOfName() {
        val pattern1 = NamePattern.ofName(Name.parse("a.b.c"))
        assertTrue(Name.parse("a.b.c").matches(pattern1))
        assertFalse(Name.parse("a.b").matches(pattern1))
        assertFalse(Name.parse("a.b.c.d").matches(pattern1))

        val pattern2 = NamePattern.ofName(Name.parse("a.*.c"))
        assertTrue(Name.parse("a.b.c").matches(pattern2))
        assertTrue(Name.parse("a.d.c").matches(pattern2))
        assertFalse(Name.parse("a.c").matches(pattern2))
        assertTrue(Name.parse("a.b[1].c").matches(pattern2))

        val pattern3 = NamePattern.ofName(Name.parse("a.b.**"))
        assertTrue(Name.parse("a.b").matches(pattern3))
        assertTrue(Name.parse("a.b.c").matches(pattern3))
        assertTrue(Name.parse("a.b.c.d").matches(pattern3))
        assertFalse(Name.parse("a.c").matches(pattern3))

        val pattern4 = NamePattern.ofName(Name.parse("**.c.d"))
        assertTrue(Name.parse("c.d").matches(pattern4))
        assertTrue(Name.parse("a.c.d").matches(pattern4))
        assertTrue(Name.parse("a.b.c.d").matches(pattern4))
        assertFalse(Name.parse("a.b.c").matches(pattern4))

        val pattern5 = NamePattern.ofName(Name.parse("a.^b\\.*$.c"))
        assertTrue(Name.parse("a.b.c").matches(pattern5))
        assertTrue(Name.parse("a.ba.c").matches(pattern5))
        assertFalse(Name.parse("a.d.c").matches(pattern5))

        val pattern6 = NamePattern.ofName(Name.parse("a[1].b"))
        assertTrue(Name.parse("a[1].b").matches(pattern6))
        assertFalse(Name.parse("a[2].b").matches(pattern6))
        assertFalse(Name.parse("a.b").matches(pattern6))

        val pattern7 = NamePattern.ofName(Name.parse("a[*].b"))
        assertTrue(Name.parse("a[1].b").matches(pattern7))
        assertTrue(Name.parse("a[2].b").matches(pattern7))
        assertTrue(Name.parse("a.b").matches(pattern7))

        val pattern8 = NamePattern.ofName(Name.parse("a[^[0-9]+$].b"))
        assertTrue(Name.parse("a[1].b").matches(pattern8))
        assertTrue(Name.parse("a[123].b").matches(pattern8))
        assertFalse(Name.parse("a[abc].b").matches(pattern8))
        assertFalse(Name.parse("a.b").matches(pattern8))
    }
}
