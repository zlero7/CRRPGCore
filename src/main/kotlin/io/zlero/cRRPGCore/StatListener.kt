package io.zlero.cRRPGCore

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;

class StatListener(private val plugin: CRRPGCorePlugin) : Listener {

    /**
     * 힘(STRENGTH): 플레이어가 공격 시 최종 데미지에 힘 보너스 가산
     * HIGHEST 우선순위 → 다른 플러그인의 데미지 수정 후 최종 합산
     */
    // onAttack(HIGH) 이후, onDefend(HIGHEST) 이전에 힘 보너스 추가되도록
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val bonus    = plugin.statManager.getBonusDamage(attacker)
        if (bonus > 0) event.damage += bonus
    }

    /**
     * 민첩(AGILITY): 플레이어가 피격 시 회피 판정
     * LOWEST 우선순위 → 가장 먼저 처리하여 회피 시 이벤트 취소
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onDamageReceive(event: EntityDamageEvent) {
        val victim = event.entity as? Player ?: return
        if (plugin.statManager.rollDodge(victim)) {
            event.isCancelled = true
            victim.sendMessage(plugin.msgCfg.msgDodgeSuccess)
        }
    }

    @EventHandler
    fun onArmorChange(event: PlayerArmorChangeEvent) {
        plugin.statManager.applyVitality(event.player)
    }

    /**
     * 체력(VITALITY): 접속 시 최대 HP 속성 재적용
     * (서버 재시작 후 HP 속성이 초기화되는 것 방지)
     */
}