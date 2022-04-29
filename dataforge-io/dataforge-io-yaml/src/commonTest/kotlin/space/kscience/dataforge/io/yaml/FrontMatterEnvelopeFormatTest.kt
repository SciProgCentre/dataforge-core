package space.kscience.dataforge.io.yaml

import space.kscience.dataforge.context.Context
import space.kscience.dataforge.io.io
import space.kscience.dataforge.io.readEnvelope
import space.kscience.dataforge.io.toByteArray
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import kotlin.test.Test
import kotlin.test.assertEquals

internal class FrontMatterEnvelopeFormatTest {

    val context = Context {
        plugin(YamlPlugin)
    }

    @Test
    fun frontMatter(){
        val text = """
            ---
            content_type: magprog
            magprog_section: contacts
            section_title: Контакты
            language: ru
            ---
            Some text here
        """.trimIndent()

        val envelope = context.io.readEnvelope(text)
        assertEquals("Some text here", envelope.data!!.toByteArray().decodeToString().trim())
        assertEquals("magprog", envelope.meta["content_type"].string)
    }
}