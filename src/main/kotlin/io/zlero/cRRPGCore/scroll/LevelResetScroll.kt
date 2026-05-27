package io.zlero.cRRPGCore.scroll

import io.zlero.cRRPGCore.CRRPGCorePlugin
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

object LevelResetScroll : Listener {

    val KEY = NamespacedKey(CRRPGCorePlugin.plugin, "level_reset_scroll")

    fun createItem(amount: Int = 1): ItemStack {
        val mc    = CRRPGCorePlugin.plugin.msgCfg
        val matStr = mc.levelResetScrollMat
        val mat   = runCatching { Material.valueOf(matStr.uppercase()) }.getOrDefault(Material.PAPER)
        val stack = ItemStack(mat, amount.coerceIn(1, 64))
        val meta: ItemMeta = stack.itemMeta
        meta.setDisplayName(mc.levelResetScrollName)
        meta.lore = mc.levelResetScrollLore
        meta.persistentDataContainer.set(KEY, PersistentDataType.BYTE, 1)
        meta.isUnbreakable = true
        if (mc.levelResetScrollCMD > 0) meta.setCustomModelData(mc.levelResetScrollCMD)
        stack.itemMeta = meta
        return stack
    }

    fun isLevelResetScroll(stack: ItemStack?): Boolean {
        if (stack == null || stack.type == Material.AIR) return false
        val meta = stack.itemMeta ?: return false
        return meta.persistentDataContainer.has(KEY, PersistentDataType.BYTE)
    }

    @EventHandler
    fun onUse(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR &&
            event.action != Action.RIGHT_CLICK_BLOCK) return

        val player = event.player
        val item   = player.inventory.itemInMainHand

        if (!isLevelResetScroll(item)) return
        event.isCancelled = true

        val plugin     = CRRPGCorePlugin.plugin
        val mc         = plugin.msgCfg
        val data       = plugin.levelManager.getPlayerData(player)
        val basePoints = plugin.statManager.pointsPerLevel

        if (data.level <= 1 && data.xp == 0L &&
            data.statPoints == basePoints &&
            data.strength == 0 && data.vitality == 0 && data.agility == 0) {
            player.sendMessage(mc.errNoLevelToReset)
            return
        }

        data.level      = 1
        data.xp         = 0L
        data.statPoints = basePoints
        data.strength   = 0
        data.vitality   = 0
        data.agility    = 0

        plugin.statManager.applyVitality(player, data)
        plugin.levelManager.savePlayerData(player, data.level, data.xp)

        if (item.amount > 1) item.amount -= 1
        else player.inventory.setItemInMainHand(null)

        player.sendMessage(mc.msgLevelResetOk)
        player.playSound(player.location, org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.8f)
    }
}
