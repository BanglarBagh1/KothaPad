package com.example.engine.pipeline

import java.util.Locale

class AutoCapitalizePipelineStep(
    override val isEnabled: Boolean = true
) : PipelineStep {
    override val name: String = "AutoCapitalize"

    override suspend fun process(inputText: String, context: PipelineContext): String {
        if (!isEnabled || inputText.isEmpty()) return inputText

        // Only capitalize for scripts with case distinctions (e.g. Latin-based)
        val isBengali = context.languageCode.startsWith("bn", ignoreCase = true)
        if (isBengali) return inputText

        val sb = StringBuilder()
        var capitalizeNext = shouldCapitalizeInitial(context.precedingText)

        val locale = try {
            Locale.forLanguageTag(context.languageCode)
        } catch (_: Exception) {
            Locale.getDefault()
        }

        for (i in inputText.indices) {
            val c = inputText[i]
            if (capitalizeNext && c.isLetter()) {
                sb.append(c.titlecase(locale))
                capitalizeNext = false
            } else {
                sb.append(c)
                if (c == '.' || c == '?' || c == '!' || c == '।' || c == '\n') {
                    capitalizeNext = true
                }
            }
        }

        return sb.toString()
    }

    private fun shouldCapitalizeInitial(precedingText: String): Boolean {
        if (precedingText.isBlank()) return true
        val trimmed = precedingText.trimEnd()
        if (trimmed.isEmpty()) return true
        val last = trimmed.last()
        return last == '.' || last == '?' || last == '!' || last == '।' || last == '\n'
    }
}
