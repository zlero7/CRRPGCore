package io.zlero.cRRPGCore

import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey
import kotlin.random.Random

class UpgradeManager(private val plugin: CRRPGCorePlugin) {

    val keyLevel        = NamespacedKey(plugin, "rpg_upgrade_level")
    val keyStoneType    = NamespacedKey(plugin, "rpg_stone_type")
    val keyProtectBreak = NamespacedKey(plugin, "rpg_protect_break")
    val keyProtectDown  = NamespacedKey(plugin, "rpg_protect_down")

    data class UpgradeChance(
        val successPct: Double,
        val keepPct:    Double,
        val breakPct:   Double,
        val downPct:    Double
    )

    private val defaultChances = mapOf(
        1  to UpgradeChance(70.0, 30.0,  0.0,  0.0),
        2  to UpgradeChance(70.0, 30.0,  0.0,  0.0),
        3  to UpgradeChance(70.0, 30.0,  0.0,  0.0),
        4  to UpgradeChance(55.0, 20.0, 10.0, 15.0),
        5  to UpgradeChance(55.0, 20.0, 10.0, 15.0),
        6  to UpgradeChance(40.0, 10.0, 20.0, 30.0),
        7  to UpgradeChance(40.0, 10.0, 20.0, 30.0),
        8  to UpgradeChance(25.0,  5.0, 35.0, 35.0),
        9  to UpgradeChance(15.0,  5.0, 50.0, 30.0),
        10 to UpgradeChance( 8.0,  0.0, 60.0, 32.0)
    )

    private val chances = mutableMapOf<Int, UpgradeChance>()

    private val defaultDamageBonus = mapOf(
        0 to 0, 1 to 5, 2 to 11, 3 to 18, 4 to 27,
        5 to 38, 6 to 52, 7 to 68, 8 to 87, 9 to 110, 10 to 140
    )
    private val damageBonusMap = mutableMapOf<Int, Int>()

    fun loadConfig(config: FileConfiguration) {
        chances.clear()
        for (lv in 1..10) {
            val path = "upgrade.chances.$lv"
            val def  = defaultChances[lv]!!
            chances[lv] = UpgradeChance(
                successPct = config.getDouble("$path.success", def.successPct),
                keepPct    = config.getDouble("$path.keep",    def.keepPct),
                breakPct   = config.getDouble("$path.break",   def.breakPct),
                downPct    = config.getDouble("$path.down",    def.downPct)
            )
        }
        damageBonusMap.clear()
        for (lv in 0..10) {
            damageBonusMap[lv] = config.getInt("upgrade.damage-bonus.$lv",
                defaultDamageBonus[lv] ?: 0)
        }
    }

    fun getChance(targetLevel: Int): UpgradeChance =
        chances[targetLevel] ?: defaultChances[targetLevel] ?: UpgradeChance(0.0, 100.0, 0.0, 0.0)

    fun getDamageBonus(level: Int): Int =
        damageBonusMap[level.coerceIn(0, 10)] ?: defaultDamageBonus[level.coerceIn(0, 10)] ?: 0

    fun getMultiplier(level: Int): Double = 1.0 + getDamageBonus(level).toDouble() / 100.0

    enum class UpgradeOutcome { SUCCESS, FAIL_KEEP, FAIL_DOWN, FAIL_BREAK }

    data class UpgradeResult(
        val outcome:    UpgradeOutcome,
        val prevLevel:  Int,
        val newLevel:   Int,
        val resultItem: ItemStack?  // FAIL_BREAK = null, 그 외 = 강화 결과 아이템
    )

    fun tryUpgrade(item: ItemStack, hasBreakPro: Boolean, hasDownPro: Boolean): UpgradeResult? {
        val meta = item.itemMeta ?: return null
        val pdc  = meta.persistentDataContainer

        if (!pdc.has(plugin.rpgItemManager.keyGrade, PersistentDataType.STRING)) return null

        val currentLevel = pdc.get(keyLevel, PersistentDataType.INTEGER) ?: 0
        if (currentLevel >= 10) return null

        val targetLevel = currentLevel + 1
        val chance      = getChance(targetLevel)

        var successPct = chance.successPct
        var keepPct    = chance.keepPct
        var breakPct   = chance.breakPct
        var downPct    = chance.downPct

        when {
            hasBreakPro && hasDownPro -> { breakPct = 0.0; downPct = 0.0 }
            hasBreakPro               -> { breakPct = 0.0 }
            hasDownPro                -> { downPct  = 0.0 }
        }

        val roll = Random.nextDouble() * 100.0

        val outcome = when {
            roll < successPct                                -> UpgradeOutcome.SUCCESS
            roll < successPct + keepPct                      -> UpgradeOutcome.FAIL_KEEP
            roll < successPct + keepPct + breakPct           -> UpgradeOutcome.FAIL_BREAK
            roll < successPct + keepPct + breakPct + downPct -> UpgradeOutcome.FAIL_DOWN
            else                                             -> UpgradeOutcome.FAIL_KEEP
        }

        val newLevel = when (outcome) {
            UpgradeOutcome.SUCCESS   -> targetLevel
            UpgradeOutcome.FAIL_DOWN -> (currentLevel - 1).coerceAtLeast(0)
            else                     -> currentLevel
        }

        if (outcome != UpgradeOutcome.FAIL_BREAK) {
            applyLevel(item, meta, pdc, newLevel)
        }

        val resultItem = if (outcome != UpgradeOutcome.FAIL_BREAK) item.clone() else null
        return UpgradeResult(outcome, currentLevel, newLevel, resultItem)
    }

    private fun applyLevel(
        item: ItemStack,
        meta: ItemMeta,
        pdc:  org.bukkit.persistence.PersistentDataContainer,
        level: Int
    ) {
        pdc.set(keyLevel, PersistentDataType.INTEGER, level)

        val rawName = stripUpgradeSuffix(meta.displayName.ifEmpty { item.type.name })
        meta.setDisplayName(if (level > 0) "$rawName §8(§6+$level§8)" else rawName)
        item.itemMeta = meta

        val bonus    = getDamageBonus(level)
        val itemType = plugin.rpgItemManager.getItemType(item)
        val gradeId  = pdc.get(plugin.rpgItemManager.keyGrade, PersistentDataType.STRING)
        val grade    = gradeId?.let { ItemGrade.fromId(it) } ?: return

        val sm = plugin.socketManager
        val am = plugin.appraisalManager

        if (itemType == RpgItemType.WEAPON) {
            val baseWpnDmg = pdc.get(plugin.rpgItemManager.keyWeaponDamage, PersistentDataType.INTEGER)
            val displayWpnDmg = if (baseWpnDmg != null) baseWpnDmg + bonus else null
            rebuildItemBlock(item, grade, displayWpnDmg)
        } else if (itemType == RpgItemType.ARMOR) {
            rebuildItemBlock(item, grade, null)
        }
    }

    private fun rebuildItemBlock(item: ItemStack, grade: ItemGrade, weaponDamage: Int?) {
        val sm = plugin.socketManager
        val am = plugin.appraisalManager
        val pdc = item.itemMeta?.persistentDataContainer ?: return

        val appraised       = am.isAppraised(item)
        val socketRemain    = sm.getRemainingRerolls(item)
        val appraisalRemain = if (pdc.has(am.keyAppraisalMaxReroll, PersistentDataType.INTEGER))
            am.getRemainingRerolls(item) else null
        val currentStats    = parseLoreStats(item)

        val meta2 = item.itemMeta ?: return
        val lore  = (meta2.lore ?: mutableListOf()).toMutableList()
        sm.rebuildOurBlock(
            lore            = lore,
            grade           = grade,
            socketCount     = sm.getSocketCount(item),
            appraised       = appraised,
            stats           = currentStats,
            socketRemain    = socketRemain,
            appraisalRemain = appraisalRemain,
            weaponDamage    = weaponDamage,
            item            = item
        )
        meta2.lore = lore
        item.itemMeta = meta2
    }

    internal fun parseLoreStats(item: ItemStack): List<AppraisalManager.StatLine> {
        val raw = item.itemMeta?.persistentDataContainer
            ?.get(plugin.socketManager.keyStatLines, PersistentDataType.STRING)
        if (!raw.isNullOrBlank()) {
            return raw.split("::").mapNotNull {
                val parts = it.split("||")
                if (parts.size != 2) null
                else AppraisalManager.StatLine(parts[0], parts[1])
            }
        }
        // fallback: lore 파싱 (구 아이템 호환)
        return parseLoreStatsFromLore(item.itemMeta?.lore ?: emptyList())
    }

    private fun parseLoreStatsFromLore(lore: List<String>): List<AppraisalManager.StatLine> {
        val result = mutableListOf<AppraisalManager.StatLine>()
        for (line in lore) {
            if (!line.contains("§7>> §f")) continue
            val stripped  = line.trimStart()
            val afterArrow = stripped.removePrefix("§7>> §f")
            val sepIdx = afterArrow.indexOf(" §8: ")
            if (sepIdx < 0) continue
            val label = afterArrow.substring(0, sepIdx)
            val value = afterArrow.substring(sepIdx + 5)
            result.add(AppraisalManager.StatLine(label, value))
        }
        return result
    }

    private fun stripUpgradeSuffix(name: String): String {
        val suffix = Regex(""" §8\(§6\+\d+§8\)$""")
        return name.replace(suffix, "")
    }

    fun initLevel(item: ItemStack) {
        val meta = item.itemMeta ?: return
        val pdc  = meta.persistentDataContainer
        if (!pdc.has(keyLevel, PersistentDataType.INTEGER)) {
            pdc.set(keyLevel, PersistentDataType.INTEGER, 0)
            item.itemMeta = meta
        }
    }

    fun getLevel(item: ItemStack?): Int {
        val pdc = item?.itemMeta?.persistentDataContainer ?: return 0
        return pdc.get(keyLevel, PersistentDataType.INTEGER) ?: 0
    }

    fun requiredStoneType(targetLevel: Int): String = when {
        targetLevel <= 3  -> "low"
        targetLevel <= 7  -> "mid"
        else              -> "high"
    }

    fun requiredStoneName(targetLevel: Int): String {
        val mc = plugin.msgCfg
        return when {
            targetLevel <= 3  -> mc.stoneNameLow
            targetLevel <= 7  -> mc.stoneNameMid
            else              -> mc.stoneNameHigh
        }
    }

    fun createStone(type: String, amount: Int = 1): ItemStack {
        val mc = plugin.msgCfg
        val (matStr, name, lore) = when (type) {
            "low"  -> Triple(mc.upgStoneLowMat,  mc.upgStoneLowName,  mc.upgStoneLowLore)
            "mid"  -> Triple(mc.upgStoneMidMat,  mc.upgStoneMidName,  mc.upgStoneMidLore)
            else   -> Triple(mc.upgStoneHighMat, mc.upgStoneHighName, mc.upgStoneHighLore)
        }
        val cmd = when (type) {
            "low"  -> mc.upgStoneLowCMD
            "mid"  -> mc.upgStoneMidCMD
            else   -> mc.upgStoneHighCMD
        }
        val mat = runCatching { Material.valueOf(matStr.uppercase()) }.getOrDefault(Material.COBBLESTONE)
        val stack = ItemStack(mat, amount.coerceIn(1, 64))
        val meta: ItemMeta = stack.itemMeta
        meta.setDisplayName(name)
        meta.lore = lore
        meta.persistentDataContainer.set(keyStoneType, PersistentDataType.STRING, type)
        meta.isUnbreakable = true
        if (cmd > 0) meta.setCustomModelData(cmd)
        stack.itemMeta = meta
        return stack
    }

    fun createProtectScroll(type: String, amount: Int = 1): ItemStack {
        val mc = plugin.msgCfg
        val isBreak = type == "break"
        val matStr  = if (isBreak) mc.protectBreakMat  else mc.protectDownMat
        val name    = if (isBreak) mc.protectBreakName else mc.protectDownName
        val lore    = if (isBreak) mc.protectBreakLore else mc.protectDownLore
        val cmd     = if (isBreak) mc.protectBreakCMD  else mc.protectDownCMD
        val key     = if (isBreak) keyProtectBreak     else keyProtectDown

        val mat = runCatching { Material.valueOf(matStr.uppercase()) }.getOrDefault(
            if (isBreak) Material.TOTEM_OF_UNDYING else Material.SHIELD
        )
        val stack = ItemStack(mat, amount.coerceIn(1, 64))
        val meta: ItemMeta = stack.itemMeta
        meta.setDisplayName(name)
        meta.lore = lore
        meta.persistentDataContainer.set(key, PersistentDataType.BYTE, 1)
        meta.isUnbreakable = true
        if (cmd > 0) meta.setCustomModelData(cmd)
        stack.itemMeta = meta
        return stack
    }

    fun isProtectBreak(item: ItemStack?): Boolean =
        item?.itemMeta?.persistentDataContainer?.has(keyProtectBreak, PersistentDataType.BYTE) ?: false

    fun isProtectDown(item: ItemStack?): Boolean =
        item?.itemMeta?.persistentDataContainer?.has(keyProtectDown, PersistentDataType.BYTE) ?: false

    fun isUpgradeStone(item: ItemStack?): Boolean =
        item?.itemMeta?.persistentDataContainer?.has(keyStoneType, PersistentDataType.STRING) ?: false

    fun getStoneType(item: ItemStack?): String? =
        item?.itemMeta?.persistentDataContainer?.get(keyStoneType, PersistentDataType.STRING)
}
