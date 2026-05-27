package io.zlero.cRRPGCore.command

import io.zlero.cRRPGCore.CRRPGCorePlugin
import io.zlero.cRRPGCore.manager.LevelManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class LevelCommand(
    private val plugin: CRRPGCorePlugin,
    private val levelManager: LevelManager
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender, command: Command,
        label: String, args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "info" -> {
                val target: Player = if (args.size >= 2) {
                    if (!sender.hasPermission("rpglevel.admin")) {
                        sender.sendMessage("§c[!] §c권한이 없습니다."); return true
                    }
                    Bukkit.getPlayer(args[1])
                        ?: run { sender.sendMessage("§c[!] §c플레이어 §e${args[1]}§c를 찾을 수 없습니다."); return true }
                } else {
                    (sender as? Player)
                        ?: run { sender.sendMessage("§c[!] §c콘솔에서는 플레이어 이름을 지정하세요."); return true }
                }

                val data      = levelManager.getPlayerData(target)
                val required  = levelManager.getRequiredXpForLevel(data.level)  // Long
                val isMax     = data.level >= levelManager.maxLevel
                val reqStr    = if (isMax) "MAX" else required.toString()
                val remaining = if (isMax) 0L else (required - data.xp).coerceAtLeast(0L)

                sender.sendMessage("§8────────────────────────────")
                sender.sendMessage("§e${target.name}§f의 레벨 정보")
                sender.sendMessage("§7레벨: §b${data.level} §8/ §7최대: §b${levelManager.maxLevel}")
                sender.sendMessage("§7경험치: §a${data.xp} §8/ §a$reqStr")
                sender.sendMessage("§7다음 레벨까지: §d$remaining XP")
                sender.sendMessage("§8────────────────────────────")
            }

            "setlevel" -> {
                if (!sender.hasPermission("rpglevel.admin")) {
                    sender.sendMessage("§c[!] §c권한이 없습니다."); return true
                }
                if (args.size < 3) { sender.sendMessage("§c[!] §c사용법: /rpglevel setlevel <player> <level>"); return true }

                val target = Bukkit.getPlayer(args[1])
                    ?: run { sender.sendMessage("§c[!] §c플레이어를 찾을 수 없습니다."); return true }
                val level  = args[2].toIntOrNull()
                    ?: run { sender.sendMessage("§c[!] §c레벨은 숫자여야 합니다."); return true }

                levelManager.setLevel(target, level)
                sender.sendMessage("§a[!] §a${target.name}의 레벨을 §e$level§a으로 설정했습니다.")
                target.sendMessage("§a[!] §a관리자에 의해 레벨이 §e$level§a으로 설정되었습니다.")
            }

            "setxp" -> {
                if (!sender.hasPermission("rpglevel.admin")) {
                    sender.sendMessage("§c[!] §c권한이 없습니다."); return true
                }
                if (args.size < 3) { sender.sendMessage("§c[!] §c사용법: /rpglevel setxp <player> <xp>"); return true }

                val target = Bukkit.getPlayer(args[1])
                    ?: run { sender.sendMessage("§c[!] §c플레이어를 찾을 수 없습니다."); return true }
                val xp = args[2].toLongOrNull()
                    ?: run { sender.sendMessage("§c[!] §cXP는 숫자여야 합니다."); return true }

                levelManager.setXp(target, xp)
                sender.sendMessage("§a[!] §a${target.name}의 XP를 §e$xp§a으로 설정했습니다.")
            }

            "givexp" -> {
                if (!sender.hasPermission("rpglevel.admin")) {
                    sender.sendMessage("§c[!] §c권한이 없습니다."); return true
                }
                if (args.size < 3) { sender.sendMessage("§c[!] §c사용법: /rpglevel givexp <player> <xp>"); return true }

                val target = Bukkit.getPlayer(args[1])
                    ?: run { sender.sendMessage("§c[!] §c플레이어를 찾을 수 없습니다."); return true }
                val xp = args[2].toLongOrNull()
                    ?: run { sender.sendMessage("§c[!] §cXP는 숫자여야 합니다."); return true }

                levelManager.giveXp(target, xp)
                sender.sendMessage("§a[!] §a${target.name}에게 §e$xp XP§a를 지급했습니다.")
            }

            "reload" -> {
                if (!sender.hasPermission("rpglevel.admin")) {
                    sender.sendMessage("§c[!] §c권한이 없습니다."); return true
                }
                plugin.reloadConfig()
                levelManager.loadConfig(plugin.config)
                sender.sendMessage("§a[!] §aCRRPGCore 설정을 다시 불러왔습니다.")
            }

            else -> sendHelp(sender)
        }
        return true
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§8─── §bRPGLevel 명령어 §8───────────────")
        sender.sendMessage("§e/rpglevel info §8[player]")
        sender.sendMessage("§e/rpglevel setlevel §8<player> <level>")
        sender.sendMessage("§e/rpglevel setxp §8<player> <xp>")
        sender.sendMessage("§e/rpglevel givexp §8<player> <xp>")
        sender.sendMessage("§e/rpglevel reload")
        sender.sendMessage("§8──────────────────────────────")
    }

    override fun onTabComplete(
        sender: CommandSender, command: Command,
        alias: String, args: Array<out String>
    ): List<String> {
        return when (args.size) {
            1 -> listOf("info", "setlevel", "setxp", "givexp", "reload")
                .filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> if (args[0].lowercase() in listOf("setlevel", "setxp", "givexp", "info"))
                Bukkit.getOnlinePlayers().map { it.name }
                    .filter { it.startsWith(args[1], ignoreCase = true) }
            else emptyList()
            else -> emptyList()
        }
    }
}
