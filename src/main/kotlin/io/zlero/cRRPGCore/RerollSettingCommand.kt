package io.zlero.cRRPGCore

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

/**
 * /리롤설정 <각성|감정> <최대횟수|-1=무제한>
 *
 * 손에 든 아이템의 최대 재각성/재감정 횟수를 개별 설정
 * 예) /리롤설정 각성 5   → 이 아이템 재각성 최대 5회
 *     /리롤설정 감정 -1  → 이 아이템 재감정 무제한
 *     /리롤설정 감정 0   → 이 아이템 재감정 불가
 */
class RerollSettingCommand(private val plugin: CRRPGCorePlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("crrpgcore.admin")) {
            sender.sendMessage("§c권한이 없습니다.")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("§c플레이어만 사용 가능합니다.")
            return true
        }
        if (args.size < 2) {
            sender.sendMessage("§e사용법: §f/리롤설정 <§b각성§f|§6감정§f> <최대횟수 | -1=무제한>")
            return true
        }

        val type   = args[0]
        val maxVal = args[1].toIntOrNull()
        if (maxVal == null || maxVal < -1) {
            sender.sendMessage("§c올바른 숫자를 입력하세요. §7(-1=무제한, 0=불가, 1 이상=최대 횟수)")
            return true
        }

        val item = sender.inventory.itemInMainHand
        if (item.type.isAir) {
            sender.sendMessage("§c손에 아이템을 들고 사용하세요.")
            return true
        }

        // 등급 읽기 (로어 재구성에 필요)
        val gradeId = item.itemMeta?.persistentDataContainer
            ?.get(plugin.rpgItemManager.keyGrade, PersistentDataType.STRING)
        val grade = gradeId?.let { ItemGrade.fromId(it) } ?: run {
            sender.sendMessage("§cRPG 아이템이 아닙니다.")
            return true
        }

        val displayMax = if (maxVal == -1) "§b무제한" else "§e${maxVal}회"
        val sm = plugin.socketManager
        val am = plugin.appraisalManager

        when (type) {
            "각성" -> {
                sm.setMaxReroll(item, maxVal)
                rebuildLore(item, grade, sm, am)
                sender.sendMessage("§a[리롤설정] §f재각성 최대 횟수를 ${displayMax}§f으로 설정했습니다.")
            }
            "감정" -> {
                am.setMaxReroll(item, maxVal)
                rebuildLore(item, grade, sm, am)
                sender.sendMessage("§a[리롤설정] §f재감정 최대 횟수를 ${displayMax}§f으로 설정했습니다.")
            }
            else -> sender.sendMessage("§e사용법: §f/리롤설정 <§b각성§f|§6감정§f> <최대횟수>")
        }

        return true
    }

    /**
     * setMaxReroll 직후 아이템 로어를 현재 NBT 상태 그대로 다시 그린다.
     *
     * appraisalRemain 결정 규칙:
     *   - keyAppraisalMaxReroll 이 NBT에 있으면 → 해당 값으로 계산한 잔여 횟수 표시
     *   - 없으면 → null (로어에 감정 횟수 줄 숨김)
     * 이렇게 하면 각성 전에 /리롤설정 감정을 써도 줄이 바로 보이고,
     * 설정하지 않은 아이템은 줄이 나타나지 않는다.
     */
    private fun rebuildLore(
        item: org.bukkit.inventory.ItemStack,
        grade: ItemGrade,
        sm: SocketManager,
        am: AppraisalManager
    ) {
        val appraised    = am.isAppraised(item)
        val socketRemain = sm.getRemainingRerolls(item)

        // keyAppraisalMaxReroll이 실제로 NBT에 존재할 때만 횟수 줄 표시
        val hasAppraisalMax = item.itemMeta?.persistentDataContainer
            ?.has(am.keyAppraisalMaxReroll, PersistentDataType.INTEGER) == true
        val appraisalRemain: Int? = if (hasAppraisalMax) am.getRemainingRerolls(item) else null

        val stats: List<AppraisalManager.StatLine> = if (appraised) parseStats(item) else emptyList()

        sm.rebuildLore(item, grade, appraised, stats, socketRemain, appraisalRemain)
    }

    /**
     * 현재 로어에서 "  §7>> §f<label> §8: <value>" 형태의 스텟 줄을 파싱
     */
    private fun parseStats(item: org.bukkit.inventory.ItemStack): List<AppraisalManager.StatLine> {
        val lore = item.itemMeta?.lore ?: return emptyList()
        return lore.mapNotNull { line ->
            if (!line.contains("§7>> §f")) return@mapNotNull null
            val after = line.substringAfter("§7>> §f")
            val sep   = after.indexOf(" §8: ")
            if (sep < 0) return@mapNotNull null
            AppraisalManager.StatLine(
                label = after.substring(0, sep),
                value = after.substring(sep + 5)
            )
        }
    }
}