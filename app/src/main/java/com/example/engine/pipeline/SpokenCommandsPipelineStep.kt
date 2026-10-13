package com.example.engine.pipeline

import java.util.regex.Pattern

class SpokenCommandsPipelineStep(
    override val isEnabled: Boolean = true
) : PipelineStep {
    override val name: String = "SpokenCommands"

    override suspend fun process(inputText: String, context: PipelineContext): String {
        if (!isEnabled || inputText.isBlank()) return inputText

        var processed = inputText

        // Check for exact action commands first
        for (cmd in context.activeCommands) {
            if (cmd.actionType == "ACTION" && !cmd.commandCode.isNullOrEmpty()) {
                val escapedPhrase = Pattern.quote(cmd.phrase)
                // Unicode-safe word boundary: surrounded by start/whitespace/punctuation and end/whitespace/punctuation
                val pattern = Pattern.compile("(?i)(?:^|(?<=[\\s,.:;?!|।]))${escapedPhrase}(?=[\\s,.:;?!|।]|$)")
                val matcher = pattern.matcher(processed)
                if (matcher.find()) {
                    context.onTriggerAction(cmd.commandCode)
                    processed = matcher.replaceAll("").trim()
                }
            }
        }

        // Punctuation replacements
        for (cmd in context.activeCommands) {
            if (cmd.actionType == "PUNCTUATION" && cmd.replacementText.isNotEmpty()) {
                val escapedPhrase = Pattern.quote(cmd.phrase)
                val pattern = Pattern.compile("(?i)(?:^|(?<=[\\s,.:;?!|।]))${escapedPhrase}(?=[\\s,.:;?!|।]|$)")
                processed = pattern.matcher(processed).replaceAll(cmd.replacementText)
            }
        }

        return processed
    }
}
