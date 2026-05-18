package io.zlero.cRRPGCore

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerSessionListener(private val plugin: CRRPGCorePlugin) : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        plugin.jewelManager.loadSlots(event.player)
    }
}
