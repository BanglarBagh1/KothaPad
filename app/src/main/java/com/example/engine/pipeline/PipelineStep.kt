package com.example.engine.pipeline

import com.example.data.model.SpokenCommand

data class PipelineContext(
    val languageCode: String,
    val precedingText: String = "",
    val activeCommands: List<SpokenCommand> = emptyList(),
    val onTriggerAction: (String) -> Unit = {}
)

interface PipelineStep {
    val name: String
    val isEnabled: Boolean
    suspend fun process(inputText: String, context: PipelineContext): String
}
