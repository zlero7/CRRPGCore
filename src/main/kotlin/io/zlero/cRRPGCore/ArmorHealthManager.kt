package io.zlero.cRRPGCore

import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerItemHeldEvent
import java.util.UUID

/**
 * 장비 ArmorStat(생명력) + 보석 JewelStat(생명력)을
 * AttributeModifier로 GENERIC_MAX_HEALTH에 반영
 *
 * applyVitality()는 baseValue를 직접 수정하므로 여기선 ADD_NUMBER modifier 사용
 * 최종 HP = baseValue(vitality) + modifier(장비HP + 보석HP)
 */
class ArmorHealthManager(private val plugin: CRRPGCorePlugin) : Listener {

    companion object {
        private val MODIFIER_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f01234567890")
        private const val MODIFIER_NAME = "crrpgcore_armor_health"
    }

    fun applyArmorHealth(player: Player) {
        val attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH) ?: return
        val oldMax = attr.value

        attr.modifiers
            .filter { it.name == MODIFIER_NAME }
            .forEach { attr.removeModifier(it) }

        val armorHp = plugin.rpgItemManager.getTotalArmorStat(player).health.toDouble()
        val jewelHp = plugin.jewelManager.getTotalStats(player)[JewelStatType.HEALTH] ?: 0.0
        val total   = armorHp + jewelHp

        if (total > 0.0) {
            @Suppress("DEPRECATION")
            attr.addModifier(AttributeModifier(MODIFIER_UUID, MODIFIER_NAME, total, AttributeModifier.Operation.ADD_NUMBER))
        }

        val newMax = attr.value
        if (newMax > oldMax) {
            player.health = (player.health + (newMax - oldMax)).coerceAtMost(newMax)
        } else if (player.health > newMax) {
            player.health = newMax
        }
    }

    // 아머 슬롯 변경 감지 → 1틱 후 반영
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        val isArmorRelated = event.slotType == InventoryType.SlotType.ARMOR
                || (event.rawSlot in 36..39)
                || event.cursor?.let { plugin.rpgItemManager.getArmorStat(it) } != null
                || event.currentItem?.let { plugin.rpgItemManager.getArmorStat(it) } != null

        if (!isArmorRelated) return

        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            applyArmorHealth(player)
        }, 1L)
    }
}