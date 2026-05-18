package io.zlero.cRRPGCore

/**
 * 장비 스텟 데이터 클래스
 */
data class ArmorStat(
    val health  : Int    = 0,
    val defense : Double = 0.0,
    val evasion : Double = 0.0
) {
    fun toLore(grade: ItemGrade): List<String> = listOf(
        "§8──────────────────",
        "  ${grade.color}◆ §f등급 §8: ${grade.color}${grade.displayName}",
        "§8──────────────────",
        "  §7>> §f추가 생명력    §a+$health",
        "  §7>> §f방어력         §b${String.format("%.1f", defense)}%",
        "  §7>> §f회피율         §e${String.format("%.1f", evasion)}%",
        "§8──────────────────",
        "  §8[감정된 장비]"
    )
}