package io.zlero.cRRPGCore

import io.zlero.cRFramework.listener.annotation.Subscribe
import org.bukkit.event.player.PlayerJoinEvent

class PlayerSessionListener(private val plugin: CRRPGCorePlugin) {

    @Subscribe
    fun onJoin(event: PlayerJoinEvent) {
        plugin.jewelManager.loadSlots(event.player)
    }
}
