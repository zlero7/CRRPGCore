package io.zlero.cRRPGCore.model

/**
 * 무기 스텟 데이터 클래스
 */
data class WeaponStat(
    val damage      : Int    = 0,
    val critChance  : Double = 0.0,
    val critDamage  : Double = 100.0,
    val attackSpeed : Double = 0.0,
    val penetration : Double = 0.0,
    val lifeSteal   : Double = 0.0
) {
    fun toLore(grade: ItemGrade): List<String> = listOf(
        "§8──────────────────",
        "  ${grade.color}◆ §f등급 §8: ${grade.color}${grade.displayName}",
        "§8──────────────────",
        "  §7>> §f추가 데미지    §r+$damage",
        "  §7>> §f치명타 확률    §e${String.format("%.1f", critChance)}%",
        "  §7>> §f치명타 피해    §e${String.format("%.0f", critDamage)}%",
        "  §7>> §f공격 속도     §a${String.format("%.1f", attackSpeed)}%",
        "  §7>> §f관통          §b${String.format("%.1f", penetration)}%",
        "  §7>> §f흡혈          §c${String.format("%.1f", lifeSteal)}%",
        "§8──────────────────",
        "  §8[감정된 무기]"
    )
}
