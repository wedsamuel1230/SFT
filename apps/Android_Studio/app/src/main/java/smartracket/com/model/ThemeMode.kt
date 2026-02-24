package smartracket.com.model

/**
 * App-wide theme mode preference.
 *
 * - [SYSTEM]: Follow the device's system dark/light setting (default).
 * - [LIGHT]: Force light theme.
 * - [DARK]: Force dark theme.
 */
enum class ThemeMode(val code: String, val displayName: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromCode(code: String): ThemeMode {
            return entries.find { it.code == code } ?: SYSTEM
        }
    }
}
