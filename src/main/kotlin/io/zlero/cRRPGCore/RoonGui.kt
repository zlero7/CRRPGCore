package io.zlero.cRRPGCore

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

object RoonGui : Listener {

    private val ROON_SLOTS = 0..8

    private fun title() = CRRPGCorePlugin.plugin.msgCfg.guiRoonTitle

    fun open(player: Player) {
        val inv = Bukkit.createInventory(null, 9, title())
        refresh(inv, player)
        player.openInventory(inv)
    }

    private fun refresh(inv: Inventory, player: Player) {
        val mgr   = CRRPGCorePlugin.plugin.jewelManager
        val slots  = mgr.getSlots(player)
        for (i in ROON_SLOTS) {
            inv.setItem(i, slots[i] ?: emptySlot(i + 1))
        }
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (title(event.view) != title()) return
        event.isCancelled = true

        val player  = event.whoClicked as? Player ?: return
        val plugin  = CRRPGCorePlugin.plugin
        val mc      = plugin.msgCfg
        val mgr     = plugin.jewelManager
        val inv     = event.inventory
        val clicked = event.clickedInventory
        val slot    = event.slot

        if (clicked == player.inventory) {
            val item = event.currentItem ?: return
            if (!mgr.isJewel(item)) { player.sendMessage(mc.errJewelOnly2); return }
            if (!mgr.isAppraised(item)) { player.sendMessage(mc.errNotAppraisedJewel); return }

            val slots    = mgr.getSlots(player)
            val emptyIdx = slots.indexOfFirst { it == null }
            if (emptyIdx < 0) { player.sendMessage(mc.errRoonFull); return }

            val toPlace = item.clone().also { it.amount = 1 }
            mgr.setSlot(player, emptyIdx, toPlace)
            if (item.amount > 1) item.amount -= 1 else event.currentItem = null

            refresh(inv, player)
            player.playSound(player.location, org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f)
            player.sendMessage(mc.format(mc.msgRoonEquip, "slot" to (emptyIdx + 1).toString()))
            return
        }

        if (clicked == inv && slot in ROON_SLOTS) {
            val slots = mgr.getSlots(player)
            val jewel = slots[slot] ?: return
            slots[slot] = null
            player.inventory.addItem(jewel)
            refresh(inv, player)
            player.playSound(player.location, org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1f, 1f)
            player.sendMessage(mc.format(mc.msgRoonUnequip, "slot" to (slot + 1).toString()))
        }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (title(event.view) != title()) return
        event.isCancelled = true
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        if (title(event.view) != title()) return
        val player = event.player as? Player ?: return
        val plugin = CRRPGCorePlugin.plugin
        plugin.jewelManager.saveSlots(player)
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            plugin.armorHealthManager.applyArmorHealth(player)
        }, 1L)
    }

    private fun emptySlot(number: Int): ItemStack {
        val mc    = CRRPGCorePlugin.plugin.msgCfg
        val stack = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val meta: ItemMeta = stack.itemMeta
        meta.setDisplayName(mc.guiRoonEmptySlotName.replace("{number}", number.toString()))
        meta.lore = mc.guiRoonEmptySlotLore
        stack.itemMeta = meta
        return stack
    }

    private fun title(view: org.bukkit.inventory.InventoryView) =
        LegacyComponentSerializer.legacySection().serialize(view.title())
}
