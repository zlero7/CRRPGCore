package io.zlero.cRRPGCore.command

import io.zlero.cRRPGCore.CRRPGCorePlugin
import io.zlero.cRRPGCore.view.RoonView
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class RoonCommand(private val plugin: CRRPGCorePlugin) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender, command: Command,
        label: String, args: Array<out String>
    ): Boolean {
        val player = sender as? Player
            ?: run { sender.sendMessage(plugin.msgCfg.errConsoleUnavail); return true }
        RoonView.openFor(plugin, player)
        return true
    }
}
