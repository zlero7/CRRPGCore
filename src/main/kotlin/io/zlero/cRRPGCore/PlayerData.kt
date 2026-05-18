package io.zlero.cRRPGCore

data class PlayerData(
    var level: Int  = 1,
    var xp: Long    = 0L,   // Int → Long (93레벨 이상 오버플로우 방지)

    var statPoints: Int = 0,
    var strength: Int   = 0,
    var vitality: Int   = 0,
    var agility: Int    = 0
)