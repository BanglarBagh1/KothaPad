package com.example.data.model

data class LanguageItem(
    val code: String,          // e.g. "bn-BD"
    val displayName: String,   // e.g. "Bangla (Bangladesh)"
    val nativeName: String,    // e.g. "বাংলা (বাংলাদেশ)"
    val flagEmoji: String,     // e.g. "🇧🇩"
    val isBangla: Boolean = false
) {
    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            LanguageItem("bn-BD", "Bangla (Bangladesh)", "বাংলা (বাংলাদেশ)", "🇧🇩", isBangla = true),
            LanguageItem("bn-IN", "Bangla (India)", "বাংলা (ভারত)", "🇮🇳", isBangla = true),
            LanguageItem("en-US", "English (United States)", "English (US)", "🇺🇸"),
            LanguageItem("en-GB", "English (United Kingdom)", "English (UK)", "🇬🇧"),
            LanguageItem("en-IN", "English (India)", "English (India)", "🇮🇳"),
            LanguageItem("en-AU", "English (Australia)", "English (Australia)", "🇦🇺"),
            LanguageItem("en-CA", "English (Canada)", "English (Canada)", "🇨🇦"),
            LanguageItem("hi-IN", "Hindi (India)", "हिन्दी", "🇮🇳"),
            LanguageItem("ur-PK", "Urdu (Pakistan)", "اردو", "🇵🇰"),
            LanguageItem("ar-SA", "Arabic (Saudi Arabia)", "العربية", "🇸🇦"),
            LanguageItem("es-ES", "Spanish (Spain)", "Español", "🇪🇸"),
            LanguageItem("fr-FR", "French (France)", "Français", "🇫🇷"),
            LanguageItem("de-DE", "German (Germany)", "Deutsch", "🇩🇪"),
            LanguageItem("ja-JP", "Japanese (Japan)", "日本語", "🇯🇵"),
            LanguageItem("zh-CN", "Chinese (Simplified)", "简体中文", "🇨🇳")
        )

        fun findByCode(code: String): LanguageItem {
            return SUPPORTED_LANGUAGES.find { it.code.equals(code, ignoreCase = true) }
                ?: LanguageItem(code, code, code, "🌐")
        }
    }
}
