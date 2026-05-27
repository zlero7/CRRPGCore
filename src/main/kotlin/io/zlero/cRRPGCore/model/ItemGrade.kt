package io.zlero.cRRPGCore.model

enum class RpgItemType(val id: String) {
    WEAPON("WEAPON"),
    ARMOR("ARMOR");

    companion object {
        fun fromId(id: String): RpgItemType? = entries.find { it.id == id }
    }
}

enum class ItemGrade(
    val id: String,
    val displayName: String,
    val color: String
) {
    COMMON   ("common",    "일반",   "§f"),
    UNCOMMON ("uncommon",  "고급",   "§a"),
    RARE     ("rare",      "희귀",   "§9"),
    EPIC     ("epic",      "영웅",   "§5"),
    UNIQUE   ("unique",    "유니크", "§6"),
    LEGENDARY("legendary", "전설",  "§c");

    fun getPrefix(): String = "$color[$displayName] §r"

    companion object {
        fun fromId(id: String): ItemGrade? =
            entries.find { it.id.equals(id, ignoreCase = true) }

        /** 한글 displayName 으로 찾기 (RpgCoreCommand 에서 사용) */
        fun fromKorean(name: String): ItemGrade? =
            entries.find { it.displayName == name }
    }
}
