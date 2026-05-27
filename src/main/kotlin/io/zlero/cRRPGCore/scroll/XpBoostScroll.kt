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

object XpBoostScroll : Listener {

    private lateinit var keyMultiplier: NamespacedKey
    private lateinit var keyMinutes:    NamespacedKey
    private lateinit var keyScope:      NamespacedKey

    fun init(plugin: CRRPGCorePlugin) {
        keyMultiplier = NamespacedKey(plugin, "xpboost_multiplier")
        keyMinutes    = NamespacedKey(plugin, "xpboost_minutes")
        keyScope      = NamespacedKey(plugin, "xpboost_scope")
    }

    fun createScroll(multiplier: Double, minutes: Int, scope: String, amount: Int = 1): ItemStack {
        val mc       = CRRPGCorePlugin.plugin.msgCfg
        val isGlobal = scope == "global"
        val scopeLabel = if (isGlobal) mc.xpBoostScopeGlobal else mc.xpBoostScopePersonal
        val mat = runCatching { Material.valueOf(mc.xpBoostScrollMat.uppercase()) }.getOrDefault(Material.PAPER)
        val stack    = ItemStack(mat, amount.coerceIn(1, 64))
        val meta: ItemMeta = stack.itemMeta

        val multStr = String.format("%.1f", multiplier)
        meta.setDisplayName(mc.xpBoostScrollName
            .replace("{scope}", scopeLabel)
            .replace("{mult}", multStr)
            .replace("{min}", minutes.toString()))
        meta.lore = mc.xpBoostScrollLore.map { line ->
            line.replace("{scope}", scopeLabel)
                .replace("{mult}", multStr)
                .replace("{min}", minutes.toString())
        }
        meta.isUnbreakable = true
        if (mc.xpBoostScrollCMD > 0) meta.setCustomModelData(mc.xpBoostScrollCMD)
        val pdc = meta.persistentDataContainer
        pdc.set(keyMultiplier, PersistentDataType.DOUBLE,  multiplier)
        pdc.set(keyMinutes,    PersistentDataType.INTEGER, minutes)
        pdc.set(keyScope,      PersistentDataType.STRING,  scope)
        stack.itemMeta = meta
        return stack
    }

    fun isBoostScroll(item: ItemStack?): Boolean {
        val pdc = item?.itemMeta?.persistentDataContainer ?: return false
        return pdc.has(keyScope, PersistentDataType.STRING)
    }

    @EventHandler
    fun onUse(event: PlayerInteractEvent) {
        if (!event.action.name.contains("RIGHT")) return
        val item = event.item ?: return
        if (!isBoostScroll(item)) return
        event.isCancelled = true

        val player = event.player
        val plugin = CRRPGCorePlugin.plugin
        val mc     = plugin.msgCfg
        val pdc    = item.itemMeta?.persistentDataContainer ?: return

        val multiplier = pdc.get(keyMultiplier, PersistentDataType.DOUBLE)  ?: return
        val minutes    = pdc.get(keyMinutes,    PersistentDataType.INTEGER) ?: return
        val scope      = pdc.get(keyScope,      PersistentDataType.STRING)  ?: return

        val boostMgr = plugin.xpBoostManager
        val multStr  = String.format("%.1f", multiplier)

        if (scope == "global") {
            if (boostMgr.hasGlobalBoost()) {
                player.sendMessage(mc.errGlobalBoostActive)
                return
            }
            boostMgr.setGlobalBoost(multiplier, minutes)
            plugin.server.broadcastMessage(mc.format(mc.msgGlobalBoostActivated,
                "player" to player.name,
                "mult"   to multStr,
                "min"    to minutes.toString()))
        } else {
            if (!boostMgr.setPersonalBoost(player.uniqueId, multiplier, minutes)) {
                player.sendMessage(mc.errPersonalBoostActive)
                return
            }
            player.sendMessage(mc.format(mc.msgPersonalBoostActivated,
                "mult" to multStr,
                "min"  to minutes.toString()))
        }

        if (item.amount <= 1) player.inventory.setItemInMainHand(null)
        else item.amount -= 1
    }
}
