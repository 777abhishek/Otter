package com.Otter.app.util

object ReleaseNotesUtil {
    enum class Section {
        FIXES,
        IMPROVEMENTS,
        PATCHES,
        OTHER,
    }

    data class ParsedNotes(
        val fixes: List<String>,
        val improvements: List<String>,
        val patches: List<String>,
        val other: List<String>,
    )

    /**
     * Clean markdown formatting from text
     */
    private fun cleanMarkdown(text: String): String {
        return text
            // Remove bold/italic markers
            .replace("**", "")
            .replace("__", "")
            .replace("*", "")
            .replace("_", "")
            // Remove headers
            .removePrefix("#").removePrefix("##").removePrefix("###")
            // Remove strikethrough
            .replace("~~", "")
            // Remove code blocks
            .replace("`", "")
            // Remove links but keep text [text](url) -> text
            .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
            // Clean up whitespace
            .trim()
    }

    fun parse(body: String): ParsedNotes {
        val lines =
            body
                .replace("\r\n", "\n")
                .split("\n")
                .map { it.trim() }

        val fixes = mutableListOf<String>()
        val improvements = mutableListOf<String>()
        val patches = mutableListOf<String>()
        val other = mutableListOf<String>()

        var active: Section = Section.OTHER

        fun addLine(section: Section, text: String) {
            if (text.isBlank()) return
            val cleaned = cleanMarkdown(text.removePrefix("-").removePrefix("*").trim())
            if (cleaned.isBlank()) return
            when (section) {
                Section.FIXES -> fixes.add(cleaned)
                Section.IMPROVEMENTS -> improvements.add(cleaned)
                Section.PATCHES -> patches.add(cleaned)
                Section.OTHER -> other.add(cleaned)
            }
        }

        for (raw in lines) {
            if (raw.isBlank()) continue

            val lower = raw.lowercase()
            val isHeader = raw.startsWith("#")
            if (isHeader) {
                active =
                    when {
                        lower.contains("fix") -> Section.FIXES
                        lower.contains("improve") -> Section.IMPROVEMENTS
                        lower.contains("patch") -> Section.PATCHES
                        else -> Section.OTHER
                    }
                continue
            }

            if (lower == "fixes" || lower == "fix" || lower == "bugs fixed") {
                active = Section.FIXES
                continue
            }
            if (lower == "improvements" || lower == "improvement" || lower == "enhancements") {
                active = Section.IMPROVEMENTS
                continue
            }
            if (lower == "patches" || lower == "patch") {
                active = Section.PATCHES
                continue
            }

            when {
                lower.startsWith("fix:") || lower.startsWith("fixes:") -> addLine(Section.FIXES, raw.substringAfter(":"))
                lower.startsWith("improve:") || lower.startsWith("improvement:") || lower.startsWith("improvements:") ->
                    addLine(Section.IMPROVEMENTS, raw.substringAfter(":"))
                lower.startsWith("patch:") || lower.startsWith("patches:") -> addLine(Section.PATCHES, raw.substringAfter(":"))
                else -> addLine(active, raw)
            }
        }

        return ParsedNotes(
            fixes = fixes.distinct(),
            improvements = improvements.distinct(),
            patches = patches.distinct(),
            other = other.distinct(),
        )
    }
}
