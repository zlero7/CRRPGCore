package io.zlero.cRRPGCore.scroll

import io.zlero.cRRPGCore.CRRPGCorePlugin
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

object StatResetScroll : Listener {

    val KEY = NamespacedKey(CRRPGCorePlugin.plugin, "stat_reset_scroll")

    fun createItem(amount: Int = 1): ItemStack {
        val mc    = CRRPGCorePlugin.plugin.msgCfg
        val mat   = runCatching { Material.valueOf(mc.statResetScrollMat.uppercase()) }.getOrDefault(Material.PAPER)
        val stack = ItemStack(mat, amount.coerceIn(1, 64))
        val meta: ItemMeta = stack.itemMeta
        meta.setDisplayName(mc.statResetScrollName)
        meta.lore = mc.statResetScrollLore
        meta.persistentDataContainer.set(KEY, PersistentDataType.BYTE, 1)
        meta.isUnbreakable = true
        if (mc.statResetScrollCMD > 0) meta.setCustomModelData(mc.statResetScrollCMD)
        stack.itemMeta = meta
        return stack
    }

    fun isResetScroll(stack: ItemStack?): Boolean {
        if (stack == null || stack.type == Material.AIR) return false
        val meta = stack.itemMeta ?: return false
        return meta.persistentDataContainer.has(KEY, PersistentDataType.BYTE)
    }

    @EventHandler
    fun onUse(event: PlayerInteractEvent) {
        if (event.action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR &&
            event.action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return

        val player = event.player
        val item   = player.inventory.itemInMainHand

        if (!isResetScroll(item)) return
        event.isCancelled = true

        val plugin = CRRPGCorePlugin.plugin
        val mc     = plugin.msgCfg
        val data   = plugin.levelManager.getPlayerData(player)

        val totalPoints = data.strength + data.vitality + data.agility
        if (totalPoints == 0) {
            player.sendMessage(mc.errNoStatToReset)
            return
        }

        plugin.playerDataRepository.update(player.uniqueId) {
            statPoints += totalPoints
            strength    = 0
            vitality    = 0
            agility     = 0
        }
        plugin.statManager.applyVitality(player)
        plugin.playerDataRepository.flush(player.uniqueId)

        if (item.amount > 1) item.amount -= 1
        else player.inventory.setItemInMainHand(null)

        player.sendMessage(mc.msgStatResetOk)
        player.sendMessage(mc.format(mc.msgStatResetDetail,
            "points" to totalPoints.toString(),
            "remaining" to (data.statPoints + totalPoints).toString()))
        player.playSound(player.location, org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f)
    }
}
