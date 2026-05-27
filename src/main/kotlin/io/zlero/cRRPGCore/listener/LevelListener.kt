package io.zlero.cRRPGCore.listener

import io.zlero.cRRPGCore.CRRPGCorePlugin
import io.zlero.cRRPGCore.manager.LevelManager
import io.zlero.cRFramework.listener.annotation.Subscribe
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerExpChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent

class LevelListener(
    private val plugin: CRRPGCorePlugin,
    private val levelManager: LevelManager
) {

    @Subscribe(priority = EventPriority.LOWEST)
    fun onExpChange(event: PlayerExpChangeEvent) {
        val gained = event.amount
        if (gained <= 0) return
        event.amount = 0
        levelManager.giveXp(event.player, gained)
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    fun onDeath(event: PlayerDeathEvent) {
        event.droppedExp = 0
        event.keepLevel  = true
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    fun onRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val data   = levelManager.getPlayerData(player)
        org.bukkit.Bukkit.getScheduler().runTask(plugin) { _ ->
            // 리스폰 시 데이터 변경 없이 속성만 재적용 (불필요한 DB 쓰기 제거)
            plugin.statManager.applyVitality(player, data)
            plugin.armorHealthManager.applyArmorHealth(player)
        }
    }

    /**
     * HIGH 우선순위 — DatabaseModule이 NORMAL 우선순위로 등록한
     * PlayerLifecycleListener.onJoin이 먼저 실행되어 데이터를 캐시에 올린 뒤 호출됨
     */
    @Subscribe(priority = EventPriority.HIGH)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // 신규 플레이어 감지 (createDefault()가 호출된 경우)
        val isNewPlayer = plugin.playerDataRepository.consumeIsNew(player.uniqueId)
        if (isNewPlayer) {
            plugin.playerDataRepository.update(player.uniqueId) {
                statPoints += plugin.statManager.pointsPerLevel
            }
            player.sendMessage(plugin.msgCfg.format(plugin.msgCfg.msgStatPoints,
                "points" to plugin.statManager.pointsPerLevel.toString()))
        }

        val data = levelManager.getPlayerData(player)
        levelManager.savePlayerData(player, data.level, data.xp)
        plugin.statManager.applyVitality(player, data)

        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            plugin.armorHealthManager.applyArmorHealth(player)
        }, 1L)
    }

    // onPlayerQuit 제거 — PlayerRepository.onQuit()이 자동으로 저장 처리
}
