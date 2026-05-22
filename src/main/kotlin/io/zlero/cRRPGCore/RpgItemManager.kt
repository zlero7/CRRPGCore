package io.zlero.cRRPGCore

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RpgItemManager(private val plugin: CRRPGCorePlugin) {

    private val armorStatCache = ConcurrentHashMap<UUID, ArmorStat>()

    val keyGrade       = NamespacedKey(plugin, "rpg_grade")
    val keyItemType    = NamespacedKey(plugin, "rpg_item_type")
    val keyDamage      = NamespacedKey(plugin, "rpg_damage")
    val keyCritChance  = NamespacedKey(plugin, "rpg_crit_chance")
    val keyCritDamage  = NamespacedKey(plugin, "rpg_crit_damage")
    val keyAtkSpeed    = NamespacedKey(plugin, "rpg_atk_speed")
    val keyPenetration = NamespacedKey(plugin, "rpg_penetration")
    val keyLifeSteal   = NamespacedKey(plugin, "rpg_life_steal")
    val keyHealth      = NamespacedKey(plugin, "rpg_health")
    val keyDefense     = NamespacedKey(plugin, "rpg_defense")
    val keyEvasion     = NamespacedKey(plugin, "rpg_evasion")
    val keyWeaponDamage = NamespacedKey(plugin, "rpg_weapon_damage")
    val keyBound        = NamespacedKey(plugin, "rpg_bound")

    data class WeaponRange(
        val dmgMin: Int,       val dmgMax: Int,
        val critChanceMin: Double, val critChanceMax: Double,
        val critDmgMin: Double,    val critDmgMax: Double,
        val atkSpeedMin: Double,   val atkSpeedMax: Double,
        val penMin: Double,        val penMax: Double,
        val lsMin: Double,         val lsMax: Double
    )

    data class ArmorRange(
        val hpMin: Int,  val hpMax: Int,
        val defMin: Double, val defMax: Double,
        val evaMin: Double, val evaMax: Double
    )

    val weaponRanges = mutableMapOf<ItemGrade, WeaponRange>()
    val armorRanges  = mutableMapOf<ItemGrade, ArmorRange>()
    var maxDefense: Double = 90.0
        private set

    fun loadConfig(config: FileConfiguration) {
        maxDefense = config.getDouble("stat.max-defense", 90.0)

        for (grade in ItemGrade.entries) {
            val wPath = "rpg-item.weapon.${grade.name}"
            weaponRanges[grade] = WeaponRange(
                dmgMin        = config.getInt("$wPath.damage.min",        defaultWeaponDmgMin(grade)),
                dmgMax        = config.getInt("$wPath.damage.max",        defaultWeaponDmgMax(grade)),
                critChanceMin = config.getDouble("$wPath.crit-chance.min",defaultCritChanceMin(grade)),
                critChanceMax = config.getDouble("$wPath.crit-chance.max",defaultCritChanceMax(grade)),
                critDmgMin    = config.getDouble("$wPath.crit-damage.min",defaultCritDmgMin(grade)),
                critDmgMax    = config.getDouble("$wPath.crit-damage.max",defaultCritDmgMax(grade)),
                atkSpeedMin   = config.getDouble("$wPath.atk-speed.min",  defaultAtkSpeedMin(grade)),
                atkSpeedMax   = config.getDouble("$wPath.atk-speed.max",  defaultAtkSpeedMax(grade)),
                penMin        = config.getDouble("$wPath.penetration.min",defaultPenMin(grade)),
                penMax        = config.getDouble("$wPath.penetration.max",defaultPenMax(grade)),
                lsMin         = config.getDouble("$wPath.life-steal.min", defaultLsMin(grade)),
                lsMax         = config.getDouble("$wPath.life-steal.max", defaultLsMax(grade))
            )

            val aPath = "rpg-item.armor.${grade.name}"
            armorRanges[grade] = ArmorRange(
                hpMin  = config.getInt("$aPath.health.min",  defaultArmorHpMin(grade)),
                hpMax  = config.getInt("$aPath.health.max",  defaultArmorHpMax(grade)),
                defMin = config.getDouble("$aPath.defense.min", defaultDefMin(grade)),
                defMax = config.getDouble("$aPath.defense.max", defaultDefMax(grade)),
                evaMin = config.getDouble("$aPath.evasion.min", defaultEvaMin(grade)),
                evaMax = config.getDouble("$aPath.evasion.max", defaultEvaMax(grade))
            )
        }
    }

    fun isRpgItem(item: ItemStack): Boolean {
        val pdc = item.itemMeta?.persistentDataContainer ?: return false
        return pdc.has(keyGrade, PersistentDataType.STRING)
    }

    fun setGrade(item: ItemStack, grade: ItemGrade, isWeapon: Boolean): Boolean {
        val meta = item.itemMeta ?: return false
        val pdc  = meta.persistentDataContainer
        pdc.set(keyGrade,    PersistentDataType.STRING, grade.id)
        pdc.set(keyItemType, PersistentDataType.STRING, if (isWeapon) RpgItemType.WEAPON.id else RpgItemType.ARMOR.id)

        val lore = (meta.lore ?: mutableListOf()).toMutableList()
        val cutFrom = lore.indexOfFirst { it.contains("§8──") }
        if (cutFrom >= 0) lore.subList(cutFrom, lore.size).clear()

        val sm = plugin.socketManager
        val am = plugin.appraisalManager

        if (isWeapon) {
            if (!pdc.has(keyWeaponDamage, PersistentDataType.INTEGER)) {
                pdc.set(keyWeaponDamage, PersistentDataType.INTEGER, 0)
            }
        }

        if (!pdc.has(sm.keySocketMaxReroll, PersistentDataType.INTEGER)) {
            pdc.set(sm.keySocketMaxReroll, PersistentDataType.INTEGER,
                plugin.config.getInt("reroll.default-max-socket-reroll", -1))
        }
        if (!pdc.has(am.keyAppraisalMaxReroll, PersistentDataType.INTEGER)) {
            pdc.set(am.keyAppraisalMaxReroll, PersistentDataType.INTEGER,
                plugin.config.getInt("reroll.default-max-appraisal-reroll", -1))
        }
        if (!pdc.has(sm.keySocketRerollCnt, PersistentDataType.INTEGER)) {
            pdc.set(sm.keySocketRerollCnt, PersistentDataType.INTEGER, 0)
        }

        meta.lore = lore
        item.itemMeta = meta

        val socketRemain    = sm.getRemainingRerolls(item)
        val appraisalRemain = am.getRemainingRerolls(item)
        val weaponDmg       = if (isWeapon) 0 else null

        val meta2 = item.itemMeta!!
        val lore2 = (meta2.lore ?: mutableListOf()).toMutableList()
        sm.rebuildOurBlock(
            lore            = lore2,
            grade           = grade,
            socketCount     = 0,
            appraised       = false,
            stats           = emptyList(),
            socketRemain    = socketRemain,
            appraisalRemain = appraisalRemain,
            weaponDamage    = weaponDmg
        )

        meta2.lore = lore2
        item.itemMeta = meta2
        return true
    }

    fun applyWeaponStat(item: ItemStack, grade: ItemGrade, stat: WeaponStat) {
        val mc   = plugin.msgCfg
        val meta = item.itemMeta ?: return
        val pdc  = meta.persistentDataContainer
        pdc.set(keyDamage,      PersistentDataType.INTEGER, stat.damage)
        pdc.set(keyCritChance,  PersistentDataType.DOUBLE,  stat.critChance)
        pdc.set(keyCritDamage,  PersistentDataType.DOUBLE,  stat.critDamage)
        pdc.set(keyAtkSpeed,    PersistentDataType.DOUBLE,  stat.attackSpeed)
        pdc.set(keyPenetration, PersistentDataType.DOUBLE,  stat.penetration)
        pdc.set(keyLifeSteal,   PersistentDataType.DOUBLE,  stat.lifeSteal)

        // 기존 스텟 라인 제거 후 재기입 (config 라벨 기준 색코드 제거 비교)
        val lore = (meta.lore ?: mutableListOf()).toMutableList()
        lore.removeAll {
            val p = plain(it)
            p.contains(plain(mc.weaponStatDmg)) ||
            p.contains(plain(mc.weaponStatCrit)) ||
            p.contains(plain(mc.weaponStatCritDmg)) ||
            p.contains(plain(mc.weaponStatAtkSpd)) ||
            p.contains(plain(mc.weaponStatPen)) ||
            p.contains(plain(mc.weaponStatLS))
        }
        lore.add("${mc.weaponStatDmg}${stat.damage}")
        lore.add("${mc.weaponStatCrit}${String.format("%.1f", stat.critChance)}%")
        lore.add("${mc.weaponStatCritDmg}${String.format("%.0f", stat.critDamage)}%")
        lore.add("${mc.weaponStatAtkSpd}${String.format("%.1f", stat.attackSpeed)}")
        if (stat.penetration > 0) lore.add("${mc.weaponStatPen}${String.format("%.1f", stat.penetration)}%")
        if (stat.lifeSteal   > 0) lore.add("${mc.weaponStatLS}${String.format("%.1f", stat.lifeSteal)}%")

        meta.lore = lore
        item.itemMeta = meta
    }

    fun applyArmorStat(item: ItemStack, grade: ItemGrade, stat: ArmorStat) {
        val mc   = plugin.msgCfg
        val meta = item.itemMeta ?: return
        val pdc  = meta.persistentDataContainer
        pdc.set(keyHealth,  PersistentDataType.INTEGER, stat.health)
        pdc.set(keyDefense, PersistentDataType.DOUBLE,  stat.defense)
        pdc.set(keyEvasion, PersistentDataType.DOUBLE,  stat.evasion)

        val lore = (meta.lore ?: mutableListOf()).toMutableList()
        lore.removeAll {
            val p = plain(it)
            p.contains(plain(mc.armorStatHp)) ||
            p.contains(plain(mc.armorStatDef)) ||
            p.contains(plain(mc.armorStatEva))
        }
        lore.add("${mc.armorStatHp}${stat.health}")
        lore.add("${mc.armorStatDef}${String.format("%.1f", stat.defense)}%")
        if (stat.evasion > 0) lore.add("${mc.armorStatEva}${String.format("%.1f", stat.evasion)}%")

        meta.lore = lore
        item.itemMeta = meta
    }

    // ── 기본값 fallback ─────────────────────────
    private fun defaultWeaponDmgMin(g: ItemGrade) = intArrayOf(5,10,20,40,70,120)[g.ordinal]
    private fun defaultWeaponDmgMax(g: ItemGrade) = intArrayOf(10,20,40,80,130,200)[g.ordinal]
    private fun defaultCritChanceMin(g: ItemGrade) = doubleArrayOf(1.0,2.0,4.0,8.0,14.0,20.0)[g.ordinal]
    private fun defaultCritChanceMax(g: ItemGrade) = doubleArrayOf(3.0,5.0,10.0,18.0,25.0,35.0)[g.ordinal]
    private fun defaultCritDmgMin(g: ItemGrade)    = doubleArrayOf(110.0,120.0,130.0,150.0,170.0,200.0)[g.ordinal]
    private fun defaultCritDmgMax(g: ItemGrade)    = doubleArrayOf(130.0,150.0,170.0,200.0,230.0,280.0)[g.ordinal]
    private fun defaultAtkSpeedMin(g: ItemGrade)   = doubleArrayOf(1.0,1.0,1.2,1.5,1.8,2.2)[g.ordinal]
    private fun defaultAtkSpeedMax(g: ItemGrade)   = doubleArrayOf(1.5,1.8,2.0,2.5,3.0,3.5)[g.ordinal]
    private fun defaultPenMin(g: ItemGrade)  = doubleArrayOf(0.0,1.0,3.0,7.0,12.0,18.0)[g.ordinal]
    private fun defaultPenMax(g: ItemGrade)  = doubleArrayOf(2.0,5.0,10.0,18.0,25.0,35.0)[g.ordinal]
    private fun defaultLsMin(g: ItemGrade)   = doubleArrayOf(0.0,0.0,1.0,2.0,4.0,6.0)[g.ordinal]
    private fun defaultLsMax(g: ItemGrade)   = doubleArrayOf(1.0,2.0,4.0,7.0,10.0,15.0)[g.ordinal]
    private fun defaultArmorHpMin(g: ItemGrade)  = intArrayOf(10,30,60,120,200,320)[g.ordinal]
    private fun defaultArmorHpMax(g: ItemGrade)  = intArrayOf(30,70,130,220,350,500)[g.ordinal]
    private fun defaultDefMin(g: ItemGrade) = doubleArrayOf(1.0,4.0,8.0,15.0,25.0,38.0)[g.ordinal]
    private fun defaultDefMax(g: ItemGrade) = doubleArrayOf(5.0,10.0,18.0,30.0,45.0,65.0)[g.ordinal]
    private fun defaultEvaMin(g: ItemGrade) = doubleArrayOf(0.0,1.0,2.0,4.0,7.0,12.0)[g.ordinal]
    private fun defaultEvaMax(g: ItemGrade) = doubleArrayOf(2.0,4.0,7.0,12.0,18.0,25.0)[g.ordinal]

    fun getItemType(item: ItemStack): RpgItemType? {
        val raw = item.itemMeta?.persistentDataContainer
            ?.get(keyItemType, PersistentDataType.STRING) ?: return null
        return RpgItemType.fromId(raw)
    }

    fun getWeaponStat(item: ItemStack): WeaponStat? {
        val pdc = item.itemMeta?.persistentDataContainer ?: return null
        if (getItemType(item) != RpgItemType.WEAPON) return null
        return WeaponStat(
            damage      = pdc.get(keyDamage,      PersistentDataType.INTEGER) ?: 0,
            critChance  = pdc.get(keyCritChance,  PersistentDataType.DOUBLE)  ?: 0.0,
            critDamage  = pdc.get(keyCritDamage,  PersistentDataType.DOUBLE)  ?: 100.0,
            attackSpeed = pdc.get(keyAtkSpeed,    PersistentDataType.DOUBLE)  ?: 0.0,
            penetration = pdc.get(keyPenetration, PersistentDataType.DOUBLE)  ?: 0.0,
            lifeSteal   = pdc.get(keyLifeSteal,   PersistentDataType.DOUBLE)  ?: 0.0
        )
    }

    fun getArmorStat(item: ItemStack): ArmorStat? {
        val pdc = item.itemMeta?.persistentDataContainer ?: return null
        if (getItemType(item) != RpgItemType.ARMOR) return null
        return ArmorStat(
            health  = pdc.get(keyHealth,  PersistentDataType.INTEGER) ?: 0,
            defense = pdc.get(keyDefense, PersistentDataType.DOUBLE)  ?: 0.0,
            evasion = pdc.get(keyEvasion, PersistentDataType.DOUBLE)  ?: 0.0
        )
    }

    fun getTotalArmorStat(player: org.bukkit.entity.Player): ArmorStat {
        val uuid = player.uniqueId
        armorStatCache[uuid]?.let { return it }
        var totalHealth  = 0
        var totalDefense = 0.0
        var totalEvasion = 0.0
        for (piece in player.inventory.armorContents) {
            if (piece == null) continue
            val stat = getArmorStat(piece) ?: continue
            totalHealth  += stat.health
            totalDefense += stat.defense
            totalEvasion += stat.evasion
        }
        val result = ArmorStat(health = totalHealth, defense = totalDefense, evasion = totalEvasion)
        armorStatCache[uuid] = result
        return result
    }

    fun invalidateArmorCache(uuid: UUID) {
        armorStatCache.remove(uuid)
    }

    // ── 귀속 시스템 ───────────────────────────────────────────────────────
    fun isBound(item: ItemStack?): Boolean =
        item?.itemMeta?.persistentDataContainer?.has(keyBound, PersistentDataType.STRING) ?: false

    fun getBoundOwner(item: ItemStack?): UUID? {
        val raw = item?.itemMeta?.persistentDataContainer?.get(keyBound, PersistentDataType.STRING) ?: return null
        return runCatching { UUID.fromString(raw) }.getOrNull()
    }

    /** 아이템 귀속 (한 번 귀속되면 해제 불가) */
    fun bindItem(item: ItemStack, owner: UUID, ownerName: String) {
        val meta = item.itemMeta ?: return
        val pdc  = meta.persistentDataContainer
        if (pdc.has(keyBound, PersistentDataType.STRING)) return  // 이미 귀속됨
        pdc.set(keyBound, PersistentDataType.STRING, owner.toString())
        val lore = (meta.lore ?: mutableListOf()).toMutableList()
        lore.add("§r")
        lore.add("  §c※ §7귀속된 아이템 §8(§f$ownerName§8)")
        meta.lore = lore
        item.itemMeta = meta
    }

    fun getWeaponBaseDamage(item: ItemStack): Int {
        val pdc = item.itemMeta?.persistentDataContainer ?: return 0
        return pdc.get(keyWeaponDamage, PersistentDataType.INTEGER) ?: 0
    }

    fun setWeaponDamage(item: ItemStack, damage: Int): Boolean {
        val meta = item.itemMeta ?: return false
        val pdc  = meta.persistentDataContainer
        if (getItemType(item) != RpgItemType.WEAPON) return false
        pdc.set(keyWeaponDamage, PersistentDataType.INTEGER, damage)
        item.itemMeta = meta

        val upgLv      = plugin.upgradeManager.getLevel(item)
        val bonus      = plugin.upgradeManager.getDamageBonus(upgLv)
        val displayDmg = damage + bonus
        val gradeId    = pdc.get(keyGrade, PersistentDataType.STRING) ?: return true
        val grade      = ItemGrade.fromId(gradeId) ?: return true

        val sm = plugin.socketManager
        val am = plugin.appraisalManager
        val appraised       = am.isAppraised(item)
        val socketRemain    = sm.getRemainingRerolls(item)
        val appraisalRemain = if (pdc.has(am.keyAppraisalMaxReroll, PersistentDataType.INTEGER))
            am.getRemainingRerolls(item) else null
        val currentStats    = plugin.upgradeManager.parseLoreStats(item)

        val meta2 = item.itemMeta ?: return true
        val lore  = (meta2.lore ?: mutableListOf()).toMutableList()
        sm.rebuildOurBlock(
            lore            = lore,
            grade           = grade,
            socketCount     = sm.getSocketCount(item),
            appraised       = appraised,
            stats           = currentStats,
            socketRemain    = socketRemain,
            appraisalRemain = appraisalRemain,
            weaponDamage    = displayDmg
        )
        meta2.lore = lore
        item.itemMeta = meta2
        return true
    }

    private fun plain(s: String) = s.replace(COLOR_REGEX, "")

    companion object {
        private val COLOR_REGEX = Regex("§[0-9a-fk-or]")
    }
}
