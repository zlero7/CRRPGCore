package io.zlero.cRRPGCore.model

enum class JewelGrade(
    val id: String,
    val displayName: String,
    val color: String,
    val minLines: Int,
    val maxLines: Int
) {
    LOW     ("low",     "하급",   "§f", 1, 1),
    MID     ("mid",     "중급",   "§a", 1, 2),
    HIGH    ("high",    "상급",   "§9", 1, 4),
    SUPREME ("supreme", "최상급", "§5", 2, 5);

    companion object {
        fun fromId(id: String) = entries.find { it.id == id }
    }
}
