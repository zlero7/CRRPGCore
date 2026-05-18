package io.zlero.cRRPGCore

enum class JewelGrade(
    val id: String,
    val displayName: String,
    val color: String,
    val minLines: Int,
    val maxLines: Int
) {
    LOW     ("low",     "\ud558\uae09",   "\u00a7f", 1, 1),
    MID     ("mid",     "\uc911\uae09",   "\u00a7a", 1, 2),
    HIGH    ("high",    "\uc0c1\uae09",   "\u00a79", 1, 4),
    SUPREME ("supreme", "\ucd5c\uc0c1\uae09", "\u00a75", 2, 5);

    companion object {
        fun fromId(id: String) = entries.find { it.id == id }
    }
}