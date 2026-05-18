package io.zlero.cRRPGCore

/**
 * 스텟 종류 열거형
 * effectPerPoint : 스텟 1당 최종 효과 단위
 */
enum class StatType(
    val displayName: String,
    val description: String,
    val effectPerPoint: Double
) {
    STRENGTH("§c힘",       "최종 데미지 +2",    2.0),
    VITALITY("§a체력",     "최대 HP +2",         2.0),
    AGILITY ("§b민첩",     "회피율 +0.5%",       0.5);
}