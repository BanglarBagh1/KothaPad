package com.example.engine.pipeline

class TextPipeline(
    private val steps: List<PipelineStep>
) {
    suspend fun execute(rawText: String, context: PipelineContext): String {
        var result = rawText
        for (step in steps) {
            if (step.isEnabled) {
                result = step.process(result, context)
            }
        }
        return result
    }

    companion object {
        fun createDefault(
            spacingEnabled: Boolean = true,
            autoCapEnabled: Boolean = true,
            commandsEnabled: Boolean = true
        ): TextPipeline {
            val steps = listOf(
                SpokenCommandsPipelineStep(isEnabled = commandsEnabled),
                SpacingPipelineStep(isEnabled = spacingEnabled),
                AutoCapitalizePipelineStep(isEnabled = autoCapEnabled)
            )
            return TextPipeline(steps)
        }
    }
}
