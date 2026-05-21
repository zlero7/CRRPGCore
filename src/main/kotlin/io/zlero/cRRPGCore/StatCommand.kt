package io.zlero.cRRPGCore

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class StatCommand(private val plugin: CRRPGCorePlugin) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender, command: Command,
        label: String, args: Array<out String>
    ): Boolean {
        val player = sender as? Player
            ?: run { sender.sendMessage("§c[!] §c플레이어만 사용 가능합니다."); return true }

        // 인수 없으면 GUI 오픈
        if (args.isEmpty()) {
            StatView(plugin).open(player)
            return true
        }

        when (args[0].lowercase()) {
            // /스텟 info → 채팅으로 스텟 요약 출력
            "info" -> {
                sender.sendMessage("§8────────────────────────────")
                plugin.statManager.getSummary(player).forEach { sender.sendMessage(it) }
                sender.sendMessage("§8────────────────────────────")
            }
            // /스텟 reset (OP 전용) → 스텟 초기화 후 포인트 환급
            "reset" -> {
                if (!sender.hasPermission("crrpgcore.admin")) {
                    sender.sendMessage("§c[!] §c권한이 없습니다."); return true
                }
                val data = plugin.levelManager.getPlayerData(player)
                val total = data.strength + data.vitality + data.agility
                plugin.playerDataRepository.update(player.uniqueId) {
                    statPoints += total
                    strength = 0; vitality = 0; agility = 0
                }
                plugin.statManager.applyVitality(player)
                player.sendMessage("§a[!] §a스텟이 초기화되었습니다. 잔여 포인트: §e${data.statPoints + total}")
            }
            else -> StatView(plugin).open(player)
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender, command: Command,
        alias: String, args: Array<out String>
    ): List<String> {
        if (args.size == 1)
            return listOf("info", "reset").filter { it.startsWith(args[0], ignoreCase = true) }
        return emptyList()
    }
}