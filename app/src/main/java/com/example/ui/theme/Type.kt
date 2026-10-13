package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Bundled Local Fonts for Bengali and Latin scripts
val NotoSansBengaliFontFamily = FontFamily(
    Font(R.font.noto_sans_bengali, FontWeight.Normal),
    Font(R.font.noto_sans_bengali, FontWeight.Medium),
    Font(R.font.noto_sans_bengali, FontWeight.Bold)
)

val InterFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

enum class AppFontOption(val displayName: String, val fontFamily: FontFamily) {
    BENGALI("Bengali (Noto Sans)", NotoSansBengaliFontFamily),
    INTER("Latin (Inter)", InterFontFamily),
    MONOSPACE("Code (JetBrains Mono)", JetBrainsMonoFontFamily),
    SYSTEM("System Default", FontFamily.Default),
    CUSTOM("Imported Font", FontFamily.Default)
}

fun getFontFamilyByOption(optionName: String, customFontPath: String? = null): FontFamily {
    if (optionName.equals("CUSTOM", ignoreCase = true) && !customFontPath.isNullOrBlank()) {
        try {
            val file = java.io.File(customFontPath)
            if (file.exists() && file.length() > 0) {
                val typeface = android.graphics.Typeface.createFromFile(file)
                return FontFamily(typeface)
            }
        } catch (_: Exception) { }
    }
    return when (optionName.uppercase()) {
        "BENGALI" -> NotoSansBengaliFontFamily
        "INTER" -> InterFontFamily
        "JETBRAINS_MONO", "MONOSPACE" -> JetBrainsMonoFontFamily
        "SYSTEM" -> FontFamily.Default
        else -> NotoSansBengaliFontFamily
    }
}

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = NotoSansBengaliFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = NotoSansBengaliFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NotoSansBengaliFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NotoSansBengaliFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoSansBengaliFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
