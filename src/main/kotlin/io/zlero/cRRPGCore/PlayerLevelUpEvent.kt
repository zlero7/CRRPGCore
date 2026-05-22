package io.zlero.cRRPGCore

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class PlayerLevelUpEvent(
    val player: Player,
    val previousLevel: Int,
    val newLevel: Int,
    val isMaxLevel: Boolean
) : Event() {
    companion object {
        private val HANDLERS = HandlerList()
        @JvmStatic fun getHandlerList() = HANDLERS
    }
    override fun getHandlers() = HANDLERS
}
