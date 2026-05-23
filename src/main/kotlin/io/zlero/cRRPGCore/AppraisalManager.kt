package io.zlero.cRRPGCore

import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.random.Random

class AppraisalManager(private val plugin: CRRPGCorePlugin) {

    val keyAppraised          = NamespacedKey(plugin, "rpg_appraised")
    val keyAppraisalRerollCnt = NamespacedKey(plugin, "rpg_appraisal_reroll_cnt")
    val keyAppraisalMaxReroll = NamespacedKey(plugin, "rpg_appraisal_max_reroll")

    var appraisalCost: Int          = 30_000
    var appraisalRerollCost: Int    = 30_000
    var jewelReappraisalCost: Int   = 20_000
    private var defaultMaxAppraisalReroll: Int = -1
    var defaultMaxJewelReroll: Int  = -1

    fun loadConfig(config: FileConfiguration) {
        appraisalCost             = config.getInt("economy.appraisal-cost",           30_000)
        appraisalRerollCost       = config.getInt("economy.appraisal-reroll-cost",    30_000)
        jewelReappraisalCost      = config.getInt("economy.jewel-reappraisal-cost",   20_000)
        defaultMaxAppraisalReroll = config.getInt("reroll.default-max-appraisal-reroll", -1)
        defaultMaxJewelReroll     = config.getInt("reroll.default-max-jewel-reroll",     -1)
    }

    fun isAppraised(item: ItemStack): Boolean {
        val pdc = item.itemMeta?.persistentDataContainer ?: return false
        return pdc.get(keyAppraised, PersistentDataType.BYTE) == 1.toByte()
    }

    fun appraise(item: ItemStack, grade: ItemGrade, rpm: RpgItemManager): AppraisalResult {
        val sm = plugin.socketManager
        if (!sm.hasSocket(item)) return AppraisalResult.NO_SOCKET
        if (isAppraised(item))   return AppraisalResult.ALREADY_APPRAISED

        val socketCount = sm.getSocketCount(item)
        val itemType    = getItemType(item, rpm) ?: return AppraisalResult.FAIL
        val roll        = rollStats(itemType, grade, socketCount, rpm)

        val meta = item.itemMeta ?: return AppraisalResult.FAIL
        val pdc  = meta.persistentDataContainer
        pdc.set(keyAppraised,          PersistentDataType.BYTE,    1.toByte())
        pdc.set(keyAppraisalRerollCnt, PersistentDataType.INTEGER, 0)
        if (!pdc.has(keyAppraisalMaxReroll, PersistentDataType.INTEGER)) {
            pdc.set(keyAppraisalMaxReroll, PersistentDataType.INTEGER, defaultMaxAppraisalReroll)
        }
        item.itemMeta = meta

        rebuildLore(item, grade, roll)
        return AppraisalResult.SUCCESS
    }

    fun rerollAppraisal(item: ItemStack, grade: ItemGrade, rpm: RpgItemManager): AppraisalRerollResult {
        if (!isAppraised(item)) return AppraisalRerollResult.NOT_APPRAISED

        val meta = item.itemMeta ?: return AppraisalRerollResult.FAIL
        val pdc  = meta.persistentDataContainer

        val cur = pdc.get(keyAppraisalRerollCnt, PersistentDataType.INTEGER) ?: 0
        val max = pdc.get(keyAppraisalMaxReroll,  PersistentDataType.INTEGER) ?: defaultMaxAppraisalReroll
        if (max != -1 && cur >= max) return AppraisalRerollResult.MAX_REACHED

        pdc.set(keyAppraisalRerollCnt, PersistentDataType.INTEGER, cur + 1)
        item.itemMeta = meta

        val socketCount = plugin.socketManager.getSocketCount(item)
        val itemType    = getItemType(item, rpm) ?: return AppraisalRerollResult.FAIL
        val roll        = rollStats(itemType, grade, socketCount, rpm)

        rebuildLore(item, grade, roll)
        return AppraisalRerollResult.SUCCESS
    }

    fun reappraisAfterSocketReroll(item: ItemStack, grade: ItemGrade, rpm: RpgItemManager): Boolean {
        if (!isAppraised(item)) return false
        val socketCount = plugin.socketManager.getSocketCount(item)
        val itemType    = getItemType(item, rpm) ?: return false
        val roll        = rollStats(itemType, grade, socketCount, rpm)
        rebuildLore(item, grade, roll)
        return true
    }

    fun setMaxReroll(item: ItemStack, max: Int): Boolean {
        val meta = item.itemMeta ?: return false
        val pdc  = meta.persistentDataContainer
        pdc.set(keyAppraisalMaxReroll,  PersistentDataType.INTEGER, max)
        pdc.set(keyAppraisalRerollCnt,  PersistentDataType.INTEGER, 0)
        item.itemMeta = meta
        return true
    }

    fun getRemainingRerolls(item: ItemStack): Int {
        val pdc = item.itemMeta?.persistentDataContainer ?: return 0
        val cur = pdc.get(keyAppraisalRerollCnt, PersistentDataType.INTEGER) ?: 0
        val max = pdc.get(keyAppraisalMaxReroll,  PersistentDataType.INTEGER) ?: defaultMaxAppraisalReroll
        return if (max == -1) -1 else (max - cur).coerceAtLeast(0)
    }

    private fun rebuildLore(item: ItemStack, grade: ItemGrade, roll: RollResult) {
        val sm              = plugin.socketManager
        val socketRemain    = sm.getRemainingRerolls(item)
        val appraisalRemain = getRemainingRerolls(item)
        val rpm             = plugin.rpgItemManager

        when (roll) {
            is RollResult.WeaponRoll -> {
                rpm.applyWeaponStat(item, grade, roll.stat)
                val upgLv     = plugin.upgradeManager.getLevel(item)
                val upgBonus  = plugin.upgradeManager.getDamageBonus(upgLv)
                val base      = item.itemMeta?.persistentDataContainer
                    ?.get(rpm.keyWeaponDamage, PersistentDataType.INTEGER) ?: 0
                val meta = item.itemMeta ?: return
                val lore = (meta.lore ?: mutableListOf()).toMutableList()
                sm.rebuildOurBlock(lore, grade, sm.getSocketCount(item), true, roll.lines,
                    socketRemain, appraisalRemain, base + upgBonus, item = item)
                meta.lore = lore
                item.itemMeta = meta
            }
            is RollResult.ArmorRoll -> {
                rpm.applyArmorStat(item, grade, roll.stat)
                val meta = item.itemMeta ?: return
                val lore = (meta.lore ?: mutableListOf()).toMutableList()
                sm.rebuildOurBlock(lore, grade, sm.getSocketCount(item), true, roll.lines,
                    socketRemain, appraisalRemain, null, item = item)
                meta.lore = lore
                item.itemMeta = meta
            }
        }
    }

    private fun rollStats(
        itemType: RpgItemType,
        grade: ItemGrade,
        socketCount: Int,
        rpm: RpgItemManager
    ): RollResult {
        val mc = plugin.msgCfg
        return when (itemType) {
            RpgItemType.WEAPON -> {
                val r = rpm.weaponRanges[grade] ?: return RollResult.WeaponRoll(
                    WeaponStat(0, 0.0, 100.0, 0.0, 0.0, 0.0), emptyList())
                var dmg = 0; var critCh = 0.0; var critDmg = 0.0
                var atkSpd = 0.0; var pen = 0.0; var ls = 0.0
                val lines = List(socketCount) {
                    when ((0..5).random()) {
                        0    -> { val v = Random.nextInt(r.dmgMin, r.dmgMax + 1); dmg += v
                                  StatLine(mc.apprWeapDmgLabel, "§r+$v") }
                        1    -> { val v = Random.nextDouble(r.critChanceMin, r.critChanceMax); critCh += v
                                  StatLine(mc.apprWeapCritLabel, "§e${String.format("%.1f", v)}%") }
                        2    -> { val v = Random.nextDouble(r.critDmgMin, r.critDmgMax); critDmg += v
                                  StatLine(mc.apprWeapCritDLabel, "§e${String.format("%.0f", v)}%") }
                        3    -> { val v = Random.nextDouble(r.atkSpeedMin, r.atkSpeedMax); atkSpd += v
                                  StatLine(mc.apprWeapSpdLabel, "§a${String.format("%.1f", v)}") }
                        4    -> { val v = Random.nextDouble(r.penMin, r.penMax); pen += v
                                  StatLine(mc.apprWeapPenLabel, "§b${String.format("%.1f", v)}%") }
                        else -> { val v = Random.nextDouble(r.lsMin, r.lsMax); ls += v
                                  StatLine(mc.apprWeapLsLabel, "§c${String.format("%.1f", v)}%") }
                    }
                }
                RollResult.WeaponRoll(WeaponStat(dmg, critCh, critDmg, atkSpd, pen, ls), lines)
            }
            RpgItemType.ARMOR -> {
                val r = rpm.armorRanges[grade] ?: return RollResult.ArmorRoll(
                    ArmorStat(0, 0.0, 0.0), emptyList())
                var hp = 0; var def = 0.0; var eva = 0.0
                val lines = List(socketCount) {
                    when ((0..2).random()) {
                        0    -> { val v = Random.nextInt(r.hpMin, r.hpMax + 1); hp += v
                                  StatLine(mc.apprArmHpLabel, "§a+$v") }
                        1    -> { val v = Random.nextDouble(r.defMin, r.defMax); def += v
                                  StatLine(mc.apprArmDefLabel, "§b${String.format("%.1f", v)}%") }
                        else -> { val v = Random.nextDouble(r.evaMin, r.evaMax); eva += v
                                  StatLine(mc.apprArmEvaLabel, "§e${String.format("%.1f", v)}%") }
                    }
                }
                RollResult.ArmorRoll(ArmorStat(hp, def, eva), lines)
            }
        }
    }

    private fun getItemType(item: ItemStack, rpm: RpgItemManager): RpgItemType? =
        rpm.getItemType(item)

    data class StatLine(val label: String, val value: String)

    sealed class RollResult {
        abstract val lines: List<StatLine>
        data class WeaponRoll(val stat: WeaponStat, override val lines: List<StatLine>) : RollResult()
        data class ArmorRoll(val stat: ArmorStat,   override val lines: List<StatLine>) : RollResult()
    }

    enum class AppraisalResult       { SUCCESS, NO_SOCKET, ALREADY_APPRAISED, FAIL }
    enum class AppraisalRerollResult { SUCCESS, NOT_APPRAISED, MAX_REACHED, FAIL }
}
