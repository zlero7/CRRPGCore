package io.zlero.cRRPGCore

import io.zlero.cRGuild.CRGuildAPI
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

class ActionBarManager(private val plugin: CRRPGCorePlugin) {

    private var task: BukkitTask? = null

    // CRGuild 플러그인 연동 여부 (서버 시작 시 1회 체크)
    private val crGuildEnabled: Boolean by lazy {
        Bukkit.getPluginManager().getPlugin("CRGuild")?.isEnabled == true
    }

    fun start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            Bukkit.getOnlinePlayers().forEach { updateActionBar(it) }
        }, 0L, 20L) // 1초(20틱) 마다 갱신
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    private fun updateActionBar(player: Player) {
        val data      = plugin.levelManager.getPlayerData(player)
        val maxHp     = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
        val currentHp = player.health

        val level = data.level
        val xp    = data.xp
        val maxXp = plugin.levelManager.getRequiredXpForLevel(level)

        val hpColor = when {
            currentHp / maxHp > 0.6 -> "§a"
            currentHp / maxHp > 0.3 -> "§e"
            else                     -> "§c"
        }

        val xpDisplay  = if (level >= plugin.levelManager.maxLevel) "§6MAX" else "§b$xp §7/ §b$maxXp"
        val guildName  = if (crGuildEnabled) CRGuildAPI.getGuildName(player) else null

        // 부스트 세그먼트
        val boostSegment = plugin.xpBoostManager.getActionBarSegment(player.uniqueId)

        val text = "${hpColor}[ ❤ ${String.format("%.1f", currentHp)} §7/ §f${String.format("%.1f", maxHp)} ${hpColor}]" +
                "  §e[ Lv.$level ]" +
                "  §7[ $xpDisplay §7]" +
                (if (boostSegment != null) "  $boostSegment" else "") +
                (if (guildName != null) "  §a[ $guildName ]" else "")

        player.sendActionBar(Component.text(text))
    }
}