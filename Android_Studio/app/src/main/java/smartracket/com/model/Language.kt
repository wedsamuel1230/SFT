package smartracket.com.model

/**
 * Supported app languages.
 */
enum class Language(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    SIMPLIFIED_CHINESE("zh-CN", "Simplified Chinese", "简体中文"),
    TRADITIONAL_CHINESE("zh-TW", "Traditional Chinese", "繁體中文");

    companion object {
        fun fromCode(code: String): Language {
            return entries.find { it.code == code } ?: ENGLISH
        }
    }
}

/**
 * Helper to get localized text based on the selected language.
 */
fun Language.localizedText(
    english: String,
    simplifiedChinese: String,
    traditionalChinese: String
): String = when (this) {
    Language.ENGLISH -> english
    Language.SIMPLIFIED_CHINESE -> simplifiedChinese
    Language.TRADITIONAL_CHINESE -> traditionalChinese
}
