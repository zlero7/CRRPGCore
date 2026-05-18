package io.zlero.cRRPGCore

import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

class AppraisalCommand(private val plugin: CRRPGCorePlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val mc = plugin.msgCfg
        if (sender !is Player) { sender.sendMessage(mc.errPlayerOnly); return true }

        val item = sender.inventory.itemInMainHand
        if (item.type.isAir) { sender.sendMessage(mc.errNeedItemInHand); return true }

        val rpm = plugin.rpgItemManager
        val am  = plugin.appraisalManager
        val jm  = plugin.jewelManager
        val eco = plugin.economy

        // ── 보석 감정 ──
        if (jm.isJewel(item)) {
            if (jm.isAppraised(item)) { sender.sendMessage(mc.errJewelAlreadyApp); return true }
            val cost = am.appraisalCost
            if (eco != null && !eco.has(sender, cost.toDouble())) {
                sender.sendMessage(mc.format(mc.errNotEnoughMoneyApp, "cost" to cost.toString()))
                return true
            }
            if (jm.appraise(item)) {
                eco?.withdrawPlayer(sender, cost.toDouble())
                sender.sendMessage(mc.format(mc.msgJewelAppraisalOk, "cost" to cost.toString()))
                sender.playSound(sender.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f)
            } else {
                sender.sendMessage(mc.errJewelAppraisalFail)
            }
            return true
        }

        // ── RPG 아이템 감정 ──
        val isReroll = command.name.equals("감정리롤", ignoreCase = true)

        val gradeId = item.itemMeta?.persistentDataContainer
            ?.get(rpm.keyGrade, PersistentDataType.STRING)
        val grade = gradeId?.let { ItemGrade.fromId(it) } ?: run {
            sender.sendMessage(mc.errNotRpgItem)
            return true
        }

        if (!isReroll) {
            val cost = am.appraisalCost
            if (eco != null && !eco.has(sender, cost.toDouble())) {
                sender.sendMessage(mc.format(mc.errNotEnoughMoneyApp, "cost" to cost.toString()))
                return true
            }
            when (am.appraise(item, grade, rpm)) {
                AppraisalManager.AppraisalResult.SUCCESS -> {
                    eco?.withdrawPlayer(sender, cost.toDouble())
                    val socketCnt = plugin.socketManager.getSocketCount(item)
                    val remaining = am.getRemainingRerolls(item)
                    val remText   = if (remaining == -1) "무제한" else "${remaining}회 남음"
                    sender.sendMessage(mc.format(mc.msgAppraisalSuccess,
                        "grade"     to "${grade.color}[${grade.displayName}]",
                        "slots"     to socketCnt.toString(),
                        "remaining" to remText,
                        "cost"      to cost.toString()))
                    sender.playSound(sender.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f)
                }
                AppraisalManager.AppraisalResult.NO_SOCKET ->
                    sender.sendMessage(mc.errNoSocket)
                AppraisalManager.AppraisalResult.ALREADY_APPRAISED ->
                    sender.sendMessage(mc.errAlreadyAppraised)
                AppraisalManager.AppraisalResult.FAIL ->
                    sender.sendMessage(mc.errNotAppraisedCmdFail)
            }
        } else {
            val cost = am.appraisalRerollCost
            if (eco != null && !eco.has(sender, cost.toDouble())) {
                sender.sendMessage(mc.format(mc.errNotEnoughMoneyReapp, "cost" to cost.toString()))
                return true
            }
            when (am.rerollAppraisal(item, grade, rpm)) {
                AppraisalManager.AppraisalRerollResult.SUCCESS -> {
                    eco?.withdrawPlayer(sender, cost.toDouble())
                    val socketCnt = plugin.socketManager.getSocketCount(item)
                    val remaining = am.getRemainingRerolls(item)
                    val remText   = if (remaining == -1) "무제한" else "${remaining}회 남음"
                    sender.sendMessage(mc.format(mc.msgReappraisalSuccess,
                        "slots"     to socketCnt.toString(),
                        "remaining" to remText,
                        "cost"      to cost.toString()))
                    sender.playSound(sender.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.8f)
                }
                AppraisalManager.AppraisalRerollResult.NOT_APPRAISED ->
                    sender.sendMessage(mc.errNotAppraised)
                AppraisalManager.AppraisalRerollResult.MAX_REACHED ->
                    sender.sendMessage(mc.errAppraisalMaxReached)
                AppraisalManager.AppraisalRerollResult.FAIL ->
                    sender.sendMessage(mc.errAppraisalFail)
            }
        }

        return true
    }
}
