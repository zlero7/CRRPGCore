package io.zlero.cRRPGCore

import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import kotlin.random.Random

class StatManager(private val plugin: CRRPGCorePlugin) {

    var pointsPerLevel: Int = 3
        private set
    var maxStrength: Int = 100
        private set
    var maxVitality: Int = 100
        private set
    var maxAgility: Int  = 100
        private set

    fun loadConfig() {
        val cfg = plugin.config
        pointsPerLevel = cfg.getInt("stat.points-per-level", 3).coerceAtLeast(1)
        maxStrength    = cfg.getInt("stat.max-strength",    100).coerceAtLeast(1)
        maxVitality    = cfg.getInt("stat.max-vitality",    100).coerceAtLeast(1)
        maxAgility     = cfg.getInt("stat.max-agility",     100).coerceAtLeast(1)
    }

    fun grantPoints(player: Player, amount: Int = pointsPerLevel) {
        val data = plugin.levelManager.getPlayerData(player)
        data.statPoints += amount
        player.sendMessage(plugin.msgCfg.format(plugin.msgCfg.msgStatPoints, "points" to amount.toString()))
    }

    fun allocate(player: Player, stat: StatType, amount: Int = 1): Boolean {
        if (amount <= 0) return false
        val data = plugin.levelManager.getPlayerData(player)
        if (data.statPoints < amount) return false

        val current = when (stat) {
            StatType.STRENGTH -> data.strength
            StatType.VITALITY -> data.vitality
            StatType.AGILITY  -> data.agility
        }
        val max = when (stat) {
            StatType.STRENGTH -> maxStrength
            StatType.VITALITY -> maxVitality
            StatType.AGILITY  -> maxAgility
        }
        if (current + amount > max) {
            player.sendMessage(plugin.msgCfg.format(plugin.msgCfg.errStatMaxReached,
                "stat" to stat.displayName, "max" to max.toString()))
            return false
        }

        data.statPoints -= amount
        when (stat) {
            StatType.STRENGTH -> data.strength += amount
            StatType.VITALITY -> { data.vitality += amount; applyVitality(player, data) }
            StatType.AGILITY  -> data.agility += amount
        }
        plugin.playerDataManager.savePlayerAsync(player.uniqueId, data)
        return true
    }

    fun getBonusDamage(player: Player): Double =
        plugin.levelManager.getPlayerData(player).strength * StatType.STRENGTH.effectPerPoint

    fun applyVitality(player: Player, data: PlayerData = plugin.levelManager.getPlayerData(player)) {
        val attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH) ?: return
        val oldBase = attr.baseValue
        val newBase = 20.0 + data.vitality * StatType.VITALITY.effectPerPoint
        attr.baseValue = newBase

        if (newBase > oldBase) {
            val gain = newBase - oldBase
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                if (!player.isOnline) return@Runnable
                val maxHp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: return@Runnable
                player.health = (player.health + gain).coerceAtMost(maxHp)
            }, 1L)
        } else if (player.health > attr.value) {
            player.health = attr.value
        }
    }

    fun rollDodge(player: Player): Boolean {
        val data      = plugin.levelManager.getPlayerData(player)
        val dodgeRate = (data.agility * StatType.AGILITY.effectPerPoint / 100.0).coerceIn(0.0, 0.75)
        return Random.nextDouble() < dodgeRate
    }

    fun getSummary(player: Player): List<String> {
        val data     = plugin.levelManager.getPlayerData(player)
        val dodgePct = String.format("%.1f", data.agility * StatType.AGILITY.effectPerPoint)
        val bonusDmg = (data.strength * StatType.STRENGTH.effectPerPoint).toInt()
        val bonusHp  = (data.vitality * StatType.VITALITY.effectPerPoint).toInt()
        return listOf(
            "§7미분배 포인트: §e${data.statPoints}",
            "§c힘 §8[§f${data.strength}§8/§7$maxStrength§8]  §7→ 데미지 §c+$bonusDmg",
            "§a체력 §8[§f${data.vitality}§8/§7$maxVitality§8]  §7→ 최대 HP §a+$bonusHp",
            "§b민첩 §8[§f${data.agility}§8/§7$maxAgility§8]  §7→ 회피율 §b$dodgePct%"
        )
    }
}
