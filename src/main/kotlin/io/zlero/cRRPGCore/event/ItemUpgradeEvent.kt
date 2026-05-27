package io.zlero.cRRPGCore.event

import io.zlero.cRRPGCore.manager.UpgradeManager
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack

class ItemUpgradeEvent(
    val player: Player,
    val item: ItemStack,
    val outcome: UpgradeManager.UpgradeOutcome,
    val previousLevel: Int,
    val newLevel: Int
) : Event() {
    companion object {
        private val HANDLERS = HandlerList()
        @JvmStatic fun getHandlerList() = HANDLERS
    }
    override fun getHandlers() = HANDLERS
}
