package io.zlero.cRRPGCore

import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class SocketManager(private val plugin: CRRPGCorePlugin) {

    val keySocketCount     = NamespacedKey(plugin, "rpg_socket_count")
    val keySocketRerollCnt = NamespacedKey(plugin, "rpg_socket_reroll_cnt")
    val keySocketMaxReroll = NamespacedKey(plugin, "rpg_socket_max_reroll")

    var socketCost: Int       = 10_000
    var socketRerollCost: Int = 10_000
    private var defaultMaxSocketReroll: Int = -1

    private val maxSocketsByGrade = mutableMapOf<ItemGrade, Int>()

    fun loadConfig(config: FileConfiguration) {
        socketCost             = config.getInt("economy.socket-cost",        10_000)
        socketRerollCost       = config.getInt("economy.socket-reroll-cost", 10_000)
        defaultMaxSocketReroll = config.getInt("reroll.default-max-socket-reroll", -1)
        for (grade in ItemGrade.entries) {
            maxSocketsByGrade[grade] = config
                .getInt("socket.max-sockets.${grade.name}", grade.ordinal + 1)
                .coerceIn(1, 6)
        }
    }

    fun getMaxSockets(grade: ItemGrade): Int = maxSocketsByGrade[grade] ?: (grade.ordinal + 1)

    fun hasSocket(item: ItemStack): Boolean {
        val pdc = item.itemMeta?.persistentDataContainer ?: return false
        return pdc.has(keySocketCount, PersistentDataType.INTEGER)
    }

    fun getSocketCount(item: ItemStack): Int {
        val pdc = item.itemMeta?.persistentDataContainer ?: return 0
        return pdc.get(keySocketCount, PersistentDataType.INTEGER) ?: 0
    }

    fun applySocket(item: ItemStack, grade: ItemGrade): SocketResult {
        val meta = item.itemMeta ?: return SocketResult.FAIL_NO_META
        val pdc  = meta.persistentDataContainer

        if (pdc.has(keySocketCount, PersistentDataType.INTEGER)) return SocketResult.ALREADY_HAS_SOCKET

        val socketCount = (1..getMaxSockets(grade)).random()

        pdc.set(keySocketCount,     PersistentDataType.INTEGER, socketCount)
        pdc.set(keySocketRerollCnt, PersistentDataType.INTEGER, 0)
        if (!pdc.has(keySocketMaxReroll, PersistentDataType.INTEGER)) {
            pdc.set(keySocketMaxReroll, PersistentDataType.INTEGER, defaultMaxSocketReroll)
        }

        item.itemMeta = meta

        val weaponDmg = pdc.get(plugin.rpgItemManager.keyWeaponDamage, PersistentDataType.INTEGER)
        val lore = (item.itemMeta!!.lore ?: mutableListOf()).toMutableList()
        val socketRemain    = getRemainingRerolls(item)
        val appraisalRemain = plugin.appraisalManager.getRemainingRerolls(item)
        rebuildOurBlock(lore, grade, socketCount, appraised = false, stats = emptyList(),
            socketRemain = socketRemain, appraisalRemain = appraisalRemain,
            weaponDamage = weaponDmg)
        val meta2 = item.itemMeta!!
        meta2.lore = lore
        item.itemMeta = meta2

        return SocketResult.SUCCESS
    }

    fun rerollSocket(item: ItemStack, grade: ItemGrade): SocketRerollResult {
        val meta = item.itemMeta ?: return SocketRerollResult.FAIL_NO_META
        val pdc  = meta.persistentDataContainer

        if (!pdc.has(keySocketCount, PersistentDataType.INTEGER)) return SocketRerollResult.NO_SOCKET

        val cur = pdc.get(keySocketRerollCnt, PersistentDataType.INTEGER) ?: 0
        val max = pdc.get(keySocketMaxReroll,  PersistentDataType.INTEGER) ?: defaultMaxSocketReroll
        if (max != -1 && cur >= max) return SocketRerollResult.MAX_REACHED

        val newCount = (1..getMaxSockets(grade)).random()
        pdc.set(keySocketCount,     PersistentDataType.INTEGER, newCount)
        pdc.set(keySocketRerollCnt, PersistentDataType.INTEGER, cur + 1)
        item.itemMeta = meta

        val weaponDmg = pdc.get(plugin.rpgItemManager.keyWeaponDamage, PersistentDataType.INTEGER)
        val socketRemain    = getRemainingRerolls(item)
        val appraisalRemain = plugin.appraisalManager.getRemainingRerolls(item)
        val lore2 = (item.itemMeta!!.lore ?: mutableListOf()).toMutableList()
        rebuildOurBlock(lore2, grade, newCount, appraised = false, stats = emptyList(),
            socketRemain = socketRemain, appraisalRemain = appraisalRemain,
            weaponDamage = weaponDmg)
        val meta2 = item.itemMeta!!
        meta2.lore = lore2
        item.itemMeta = meta2

        return SocketRerollResult.SUCCESS
    }

    fun rebuildLore(
        item: ItemStack,
        grade: ItemGrade,
        appraised: Boolean,
        stats: List<AppraisalManager.StatLine>,
        socketRemain: Int,
        appraisalRemain: Int? = null
    ) {
        val meta      = item.itemMeta ?: return
        val lore      = (meta.lore ?: mutableListOf()).toMutableList()
        val weaponDmg = meta.persistentDataContainer
            .get(plugin.rpgItemManager.keyWeaponDamage, PersistentDataType.INTEGER)
        rebuildOurBlock(lore, grade, getSocketCount(item), appraised, stats,
            socketRemain, appraisalRemain, weaponDamage = weaponDmg)
        meta.lore = lore
        item.itemMeta = meta
    }

    fun refreshLoreAfterSocketReroll(item: ItemStack, grade: ItemGrade, am: AppraisalManager) {
        val meta = item.itemMeta ?: return
        val pdc  = meta.persistentDataContainer
        val weaponDmg       = pdc.get(plugin.rpgItemManager.keyWeaponDamage, PersistentDataType.INTEGER)
        val socketRemain    = getRemainingRerolls(item)
        val appraisalRemain = am.getRemainingRerolls(item)
        val lore            = (meta.lore ?: mutableListOf()).toMutableList()
        rebuildOurBlock(
            lore            = lore,
            grade           = grade,
            socketCount     = getSocketCount(item),
            appraised       = false,
            stats           = emptyList(),
            socketRemain    = socketRemain,
            appraisalRemain = appraisalRemain,
            weaponDamage    = weaponDmg
        )
        meta.lore = lore
        item.itemMeta = meta
    }

    fun rebuildOurBlock(
        lore: MutableList<String>,
        grade: ItemGrade,
        socketCount: Int,
        appraised: Boolean,
        stats: List<AppraisalManager.StatLine>,
        socketRemain: Int,
        appraisalRemain: Int? = null,
        weaponDamage: Int? = null
    ) {
        val mc  = plugin.msgCfg
        val sep = mc.loreSeparator

        val gradeLineIdx = lore.indexOfFirst { plain(it).contains("◆") && plain(it).contains("등급") }

        val cutFrom = if (gradeLineIdx >= 0) {
            (gradeLineIdx + 1 until lore.size).firstOrNull { lore[it].contains("§8──") } ?: lore.size
        } else {
            lore.indexOfFirst { it.contains("§8──") }.takeIf { it >= 0 } ?: lore.size
        }
        if (cutFrom < lore.size) lore.subList(cutFrom, lore.size).clear()

        if (gradeLineIdx < 0) {
            lore.add(sep)
            lore.add("  ${grade.color}${mc.loreGradeLabel}${grade.color}${grade.displayName}")
        }

        if (weaponDamage != null) {
            lore.add(sep)
            lore.add("${mc.loreWeaponDmgLabel}$weaponDamage")
        }

        lore.add(sep)

        if (!appraised) {
            if (socketCount == 0) {
                lore.add(mc.loreNoSocket)
            } else {
                repeat(socketCount) { lore.add(mc.loreEmptySlot) }
                lore.add(mc.loreAppraisalGuide)
            }
        } else {
            for (stat in stats) lore.add("  §7>> §f${stat.label} §8: ${stat.value}")
        }

        lore.add(sep)
        val socketRemainStr = if (socketRemain < 0) mc.loreInfSymbol else "§e${socketRemain}회"
        lore.add(mc.loreSocketRemain.replace("{count}", socketRemainStr))
        if (appraisalRemain != null) {
            val appraisalRemainStr = if (appraisalRemain < 0) mc.loreInfSymbol else "§e${appraisalRemain}회"
            lore.add(mc.loreAppraisalRemain.replace("{count}", appraisalRemainStr))
        }

        if (appraised) {
            lore.add("")
            lore.add(mc.loreAppraised)
        }
    }

    fun playSuccess(player: Player) {
        player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f)
    }

    fun playReroll(player: Player) {
        player.playSound(player.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f)
    }

    fun playFail(player: Player) {
        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f)
    }

    fun setMaxReroll(item: ItemStack, max: Int): Boolean {
        val meta = item.itemMeta ?: return false
        val pdc  = meta.persistentDataContainer
        pdc.set(keySocketMaxReroll, PersistentDataType.INTEGER, max)
        item.itemMeta = meta
        return true
    }

    fun getRemainingRerolls(item: ItemStack): Int {
        val pdc = item.itemMeta?.persistentDataContainer ?: return 0
        val cur = pdc.get(keySocketRerollCnt, PersistentDataType.INTEGER) ?: 0
        val max = pdc.get(keySocketMaxReroll,  PersistentDataType.INTEGER) ?: defaultMaxSocketReroll
        return if (max == -1) -1 else (max - cur).coerceAtLeast(0)
    }

    private fun plain(s: String) = s.replace(COLOR_REGEX, "")

    companion object {
        private val COLOR_REGEX = Regex("§[0-9a-fk-or]")
    }

    enum class SocketResult       { SUCCESS, ALREADY_HAS_SOCKET, FAIL_NO_META }
    enum class SocketRerollResult { SUCCESS, NO_SOCKET, MAX_REACHED, FAIL_NO_META }
}
