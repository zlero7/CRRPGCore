package io.zlero.cRRPGCore

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerExpChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent

class LevelListener(
    private val plugin: CRRPGCorePlugin,
    private val levelManager: LevelManager
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onExpChange(event: PlayerExpChangeEvent) {
        val gained = event.amount
        if (gained <= 0) return
        event.amount = 0
        levelManager.giveXp(event.player, gained)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDeath(event: PlayerDeathEvent) {
        event.droppedExp = 0
        event.keepLevel  = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val data   = levelManager.getPlayerData(player)
        org.bukkit.Bukkit.getScheduler().runTask(plugin) { _ ->
            levelManager.savePlayerData(player, data.level, data.xp)
            plugin.statManager.applyVitality(player, data)
            plugin.armorHealthManager.applyArmorHealth(player)
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        val isNewPlayer = !plugin.playerDataManager.hasPlayer(player.uniqueId)
        val data = levelManager.getPlayerData(player)

        if (isNewPlayer) {
            data.statPoints += plugin.statManager.pointsPerLevel
            plugin.playerDataManager.savePlayer(player.uniqueId, data)
            player.sendMessage(plugin.msgCfg.format(plugin.msgCfg.msgStatPoints,
                "points" to plugin.statManager.pointsPerLevel.toString()))
        }

        levelManager.savePlayerData(player, data.level, data.xp)
        plugin.statManager.applyVitality(player, data)

        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            plugin.armorHealthManager.applyArmorHealth(player)
        }, 1L)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        plugin.jewelManager.removeSlots(event.player)
        levelManager.removePlayerData(event.player)
    }

}