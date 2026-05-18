package io.zlero.cRRPGCore

import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

class SocketCommand(private val plugin: CRRPGCorePlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) { sender.sendMessage("§c플레이어만 사용 가능합니다."); return true }

        val isReroll = command.name.equals("소켓리롤", ignoreCase = true)
        val item = sender.inventory.itemInMainHand

        if (item.type.isAir) { sender.sendMessage("§c손에 아이템을 들고 사용하세요."); return true }

        val rpm = plugin.rpgItemManager
        val sm  = plugin.socketManager
        val am  = plugin.appraisalManager
        val eco = plugin.economy

        // 등급 확인
        val gradeId = item.itemMeta?.persistentDataContainer
            ?.get(rpm.keyGrade, PersistentDataType.STRING)
        val grade = gradeId?.let { ItemGrade.fromId(it) } ?: run {
            sender.sendMessage("§cRPG 아이템이 아닙니다.")
            return true
        }

        if (!isReroll) {
            // ─── /소켓: 소켓 배치 ───────────────────────
            val cost = sm.socketCost
            if (eco != null && !eco.has(sender, cost.toDouble())) {
                sender.sendMessage("§c각성 비용이 부족합니다. §8(필요: §e${cost}원§8)")
                return true
            }

            when (sm.applySocket(item, grade)) {
                SocketManager.SocketResult.SUCCESS -> {
                    eco?.withdrawPlayer(sender, cost.toDouble())
                    val cnt = sm.getSocketCount(item)
                    val max = sm.getMaxSockets(grade)
                    sender.sendMessage(
                        "§a[ 각성 ] §f각성 배치 완료! §8(§e${cnt}§8/§7${max}§8 각성) §7-${cost}원\n"
                    )
                    sender.playSound(sender.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f)
                }
                SocketManager.SocketResult.ALREADY_HAS_SOCKET ->
                    sender.sendMessage("§c이미 각성이 된 아이템입니다.")
                SocketManager.SocketResult.FAIL_NO_META ->
                    sender.sendMessage("§c각성 배치 실패.")
            }

        } else {
            // ─── /소켓리롤: 소켓 수 재결정 ─────────────
            val cost = sm.socketRerollCost
            if (eco != null && !eco.has(sender, cost.toDouble())) {
                sender.sendMessage("§c재각성 비용이 부족합니다. §8(필요: §e${cost}원§8)")
                return true
            }

            val prevCount = sm.getSocketCount(item)

            when (sm.rerollSocket(item, grade)) {
                SocketManager.SocketRerollResult.SUCCESS -> {
                    eco?.withdrawPlayer(sender, cost.toDouble())
                    val newCount  = sm.getSocketCount(item)
                    val remaining = sm.getRemainingRerolls(item)
                    val remText   = if (remaining == -1) "무제한" else "${remaining}회 남음"

                    // 소켓 수가 바뀌면서 감정된 아이템이면 스텟도 자동 재감정
                    val reappraised = am.reappraisAfterSocketReroll(item, grade, rpm)
                    val reapprMsg   = if (reappraised) "\n§7각성 수 변경으로 §f스텟도 재결정§7되었습니다." else ""

                    // 로어에 소켓 수 반영 (감정 여부에 따라 다른 텍스트)
                    val meta = item.itemMeta
                    if (meta != null) {
                        val lore = (meta.lore ?: mutableListOf()).toMutableList()
                        val idx  = lore.indexOfFirst { it.contains("§8[소켓") }
                        if (idx >= 0) {
                            lore[idx] = if (reappraised)
                                "§8[각성 §e${newCount}§8] §7(스텟 ${newCount}종)"
                            else
                                "§8[각성 §e${newCount}§8] §7(스텟 슬롯 ${newCount}개)"
                        }
                        if (!reappraised) {
                            lore.removeAll { it.contains("§7▷ /감정") }
                            val socketIdx = lore.indexOfFirst { it.contains("§8[소켓") }
                            if (socketIdx >= 0) lore.add(socketIdx + 1, "§7▷ /감정 으로 스텟을 부여하세요")
                        }
                        meta.lore = lore
                        item.itemMeta = meta
                    }

                    sender.sendMessage(
                        "§b[ 재각성 ] §f${prevCount} 각성 → §e${newCount} 각성 §7($remText)  §8-${cost}원${reapprMsg}"
                    )
                    sender.playSound(sender.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.5f)
                }
                SocketManager.SocketRerollResult.NO_SOCKET ->
                    sender.sendMessage("§c각성이 존재하지 않는 아이템입니다.")
                SocketManager.SocketRerollResult.MAX_REACHED ->
                    sender.sendMessage("§c재각성 최대 횟수에 도달했습니다.")
                SocketManager.SocketRerollResult.FAIL_NO_META ->
                    sender.sendMessage("§c재각성 실패.")
            }
        }

        return true
    }
}