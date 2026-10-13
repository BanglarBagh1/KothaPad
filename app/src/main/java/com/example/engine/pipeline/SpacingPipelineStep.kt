package com.example.engine.pipeline

class SpacingPipelineStep(
    override val isEnabled: Boolean = true
) : PipelineStep {
    override val name: String = "SpacingFix"

    override suspend fun process(inputText: String, context: PipelineContext): String {
        if (!isEnabled || inputText.isEmpty()) return inputText

        var text = inputText

        // Remove space before punctuation marks: , . ? ! : ; ।
        text = text.replace(Regex("\\s+([,.:;?!|।])"), "$1")

        // Ensure single space after punctuation if followed by a letter/digit
        text = text.replace(Regex("([,.:;?!|।])([^\\s\\n0-9,.:;?!|।])"), "$1 $2")

        // Remove double/triple spaces (preserve single spaces and newlines)
        text = text.replace(Regex("[ \\t]{2,}"), " ")

        // Check boundary between preceding text and current input
        if (context.precedingText.isNotEmpty()) {
            val lastChar = context.precedingText.last()
            val firstChar = text.firstOrNull()
            if (firstChar != null && !firstChar.isWhitespace() && firstChar !in ",.:;?!|।\n") {
                if (!lastChar.isWhitespace() && lastChar != '\n') {
                    text = " $text"
                }
            }
        }

        return text
    }
}
