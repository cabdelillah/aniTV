package app.anitv.core.util

internal object Html {
    private val lineBreak = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
    private val tag = Regex("<[^>]+>")
    private val blankLines = Regex("""\n{3,}""")
    private val entities = mapOf(
        "&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"",
        "&#39;" to "'", "&apos;" to "'", "&nbsp;" to " ", "&mdash;" to "—", "&ndash;" to "–",
    )

    fun toPlainText(html: String): String {
        var text = html.replace(lineBreak, "\n").replace(tag, "")
        for ((entity, char) in entities) text = text.replace(entity, char)
        return text.replace("\r", "").replace(blankLines, "\n\n").trim()
    }
}
