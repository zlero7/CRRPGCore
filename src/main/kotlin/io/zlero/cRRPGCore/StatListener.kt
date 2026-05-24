package io.zlero.cRRPGCore

import org.bukkit.entity.Player
import io.zlero.cRFramework.listener.annotation.Subscribe
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;

class StatListener(private val plugin: CRRPGCorePlugin) {

    /**
     * 힘(STRENGTH): 플레이어가 공격 시 최종 데미지에 힘 보너스 가산
     * HIGHEST 우선순위 → RpgItemListener.onAttack(HIGH)이 RPG 데미지를 설정한 뒤 가산
     */
    @Subscribe(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val bonus    = plugin.statManager.getBonusDamage(attacker)
        if (bonus > 0) event.damage += bonus
    }

    /**
     * 민첩(AGILITY): 플레이어가 엔티티에게 공격받을 때만 회피 판정
     * LOWEST 우선순위 → 가장 먼저 처리하여 회피 시 이벤트 취소
     * EntityDamageByEntityEvent로 제한 → 낙하·화재·폭발 피해는 회피 대상 제외
     */
    @Subscribe(priority = EventPriority.LOWEST)
    fun onDamageReceive(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        if (plugin.statManager.rollDodge(victim)) {
            event.isCancelled = true
            victim.sendMessage(plugin.msgCfg.msgDodgeSuccess)
        }
    }

    @Subscribe
    fun onArmorChange(event: PlayerArmorChangeEvent) {
        plugin.statManager.applyVitality(event.player)
    }

    /**
     * 체력(VITALITY): 접속 시 최대 HP 속성 재적용
     * (서버 재시작 후 HP 속성이 초기화되는 것 방지)
     */
}