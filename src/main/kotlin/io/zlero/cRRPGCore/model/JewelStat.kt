package io.zlero.cRRPGCore.model

enum class JewelStatType(
    val key: String,
    val isInteger: Boolean = false
) {
    DAMAGE      ("damage",      isInteger = true),
    CRIT_CHANCE ("critChance"),
    CRIT_DAMAGE ("critDamage"),
    ATTACK_SPEED("attackSpeed"),
    PENETRATION ("penetration"),
    LIFE_STEAL  ("lifeSteal"),
    HEALTH      ("health",      isInteger = true),
    DEFENSE     ("defense"),
    EVASION     ("evasion");

    companion object {
        private val BY_KEY = entries.associateBy { it.key }
        fun fromKey(key: String): JewelStatType? = BY_KEY[key]
    }
}

data class JewelStat(val type: JewelStatType, val value: Double) {

    fun toLoreLine(): String {
        val label = when (type) {
            JewelStatType.DAMAGE       -> "§c추가 데미지"
            JewelStatType.CRIT_CHANCE  -> "§e치명타 확률"
            JewelStatType.CRIT_DAMAGE  -> "§e치명타 피해"
            JewelStatType.ATTACK_SPEED -> "§a공격 속도"
            JewelStatType.PENETRATION  -> "§b관통"
            JewelStatType.LIFE_STEAL   -> "§c흡혈"
            JewelStatType.HEALTH       -> "§a추가 생명력"
            JewelStatType.DEFENSE      -> "§b방어력"
            JewelStatType.EVASION      -> "§e회피율"
        }
        val fmt = if (type.isInteger) "+${value.toInt()}" else "+${String.format("%.1f", value)}%"
        return "  §8>> $label §f$fmt"
    }

    companion object {
        val ALL_TYPES = JewelStatType.entries

        fun randomValue(type: JewelStatType, grade: JewelGrade): Double = when (type) {
            JewelStatType.DAMAGE       -> randInt(grade, 1, 2, 3, 6, 8, 14, 14, 22).toDouble()
            JewelStatType.HEALTH       -> randInt(grade, 2, 4, 5, 10, 12, 22, 22, 35).toDouble()
            JewelStatType.CRIT_CHANCE  -> randDbl(grade, 0.2, 0.8, 0.5, 1.5, 1.0, 3.0, 2.5, 5.0)
            JewelStatType.CRIT_DAMAGE  -> randDbl(grade, 1.0, 4.0, 3.0, 8.0, 6.0, 15.0, 12.0, 25.0)
            JewelStatType.ATTACK_SPEED -> randDbl(grade, 0.2, 1.0, 0.5, 2.0, 1.0, 4.0, 2.5, 7.0)
            JewelStatType.PENETRATION  -> randDbl(grade, 0.1, 0.5, 0.3, 1.2, 0.8, 2.5, 1.5, 4.0)
            JewelStatType.LIFE_STEAL   -> randDbl(grade, 0.1, 0.4, 0.2, 0.8, 0.5, 1.5, 1.0, 2.5)
            JewelStatType.DEFENSE      -> randDbl(grade, 0.2, 0.8, 0.5, 1.5, 1.0, 3.0, 2.0, 5.0)
            JewelStatType.EVASION      -> randDbl(grade, 0.1, 0.5, 0.3, 1.0, 0.6, 2.0, 1.2, 3.5)
        }

        private fun randInt(
            g: JewelGrade,
            lMin: Int, lMax: Int, mMin: Int, mMax: Int,
            hMin: Int, hMax: Int, sMin: Int, sMax: Int
        ): Int {
            val (mn, mx) = when (g) {
                JewelGrade.LOW     -> lMin to lMax
                JewelGrade.MID     -> mMin to mMax
                JewelGrade.HIGH    -> hMin to hMax
                JewelGrade.SUPREME -> sMin to sMax
            }
            return (mn..mx).random()
        }

        private fun randDbl(
            g: JewelGrade,
            lMin: Double, lMax: Double, mMin: Double, mMax: Double,
            hMin: Double, hMax: Double, sMin: Double, sMax: Double
        ): Double {
            val (mn, mx) = when (g) {
                JewelGrade.LOW     -> lMin to lMax
                JewelGrade.MID     -> mMin to mMax
                JewelGrade.HIGH    -> hMin to hMax
                JewelGrade.SUPREME -> sMin to sMax
            }
            return mn + kotlin.random.Random.nextDouble() * (mx - mn)
        }
    }
}
