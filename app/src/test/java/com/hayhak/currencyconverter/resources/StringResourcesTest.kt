package com.hayhak.currencyconverter.resources

import org.junit.Assert.assertTrue
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Test

/**
 * Keeps the 20 translations complete. Adding a string to values/strings.xml without translating it
 * (or breaking a placeholder like %1$s) fails here instead of shipping English text in other languages.
 *
 * Plurals are checked for presence of the resource only; their quantity sets differ per language.
 */
class StringResourcesTest {

    private val resDir: File = listOf("src/main/res", "app/src/main/res").map(::File).first { it.isDirectory }

    private fun load(dir: File): Map<String, String> {
        val file = File(dir, "strings.xml")
        if (!file.exists()) return emptyMap()
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
        val out = LinkedHashMap<String, String>()
        val nodes = root.childNodes
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as? Element ?: continue
            if (el.tagName == "string" && el.getAttribute("translatable") != "false") {
                out[el.getAttribute("name")] = el.textContent
            }
        }
        return out
    }

    private fun placeholders(text: String): List<String> =
        Regex("""%(\d+\$)?[sdf]""").findAll(text).map { it.value.replace(Regex("""^%\d+\$"""), "%") }.sorted().toList()

    private val locales = resDir.listFiles { f -> f.isDirectory && f.name.startsWith("values-") && File(f, "strings.xml").exists() }!!
        .sortedBy { it.name }

    @Test
    fun `every translatable string exists in every language`() {
        val base = load(File(resDir, "values"))
        val problems = locales.flatMap { dir ->
            val translated = load(dir)
            base.keys.filter { it !in translated }.map { "${dir.name}: missing $it" }
        }
        assertTrue("Missing translations:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `placeholders match the source text`() {
        val base = load(File(resDir, "values"))
        val problems = locales.flatMap { dir ->
            load(dir).mapNotNull { (key, text) ->
                val source = base[key] ?: return@mapNotNull null
                if (placeholders(source) != placeholders(text)) "${dir.name}: $key has ${placeholders(text)}, source has ${placeholders(source)}" else null
            }
        }
        assertTrue("Placeholder mismatches:\n" + problems.joinToString("\n"), problems.isEmpty())
    }
}
