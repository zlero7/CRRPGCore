package io.zlero.cRRPGCore.listener

import io.zlero.cRRPGCore.CRRPGCorePlugin
import io.zlero.cRFramework.listener.annotation.Subscribe
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerItemDamageEvent

/**
 * RPG 아이템(장비/무기) 내구도 무한 리스너
 *
 * - 등급이 매겨진 RPG 아이템은 내구도 감소 이벤트를 취소한다.
 * - 감정(appraised) 여부와 무관하게 등급 NBT(rpg_grade)가 존재하면 내구도 무한.
 * - 즉, /rpgcore grade 명령어로 등급을 설정하는 순간부터 효과 적용.
 */
class RpgDurabilityListener(private val plugin: CRRPGCorePlugin) {

    @Subscribe(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onItemDamage(event: PlayerItemDamageEvent) {
        val item = event.item
        if (plugin.rpgItemManager.isRpgItem(item)) {
            event.isCancelled = true
        }
    }
}
