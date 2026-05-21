package io.zlero.cRRPGCore

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

class JewelManager(private val plugin: CRRPGCorePlugin) {

    // ── NBT 키 ──────────────────────────────────────────────────────────
    val keyJewelGrade  = NamespacedKey(plugin, "jewel_grade")      // 보석 등급 id
    val keyJewelStats  = NamespacedKey(plugin, "jewel_stats")      // 스텟 직렬화 문자열
    val keyAppraised   = NamespacedKey(plugin, "jewel_appraised")  // 감정 여부

    // 룬 슬롯은 RoonSlotRepository가 캐시 및 저장 관리

    fun getSlots(player: Player): Array<ItemStack?> =
        plugin.roonSlotRepository.get(player.uniqueId)?.slots ?: arrayOfNulls(9)

    fun setSlot(player: Player, index: Int, item: ItemStack?) {
        plugin.roonSlotRepository.update(player.uniqueId) {
            slots[index] = item
        }
    }

    /** GUI 닫을 때 즉시 저장 (dirty 데이터를 DB에 flush) */
    fun saveSlots(player: Player) {
        plugin.roonSlotRepository.flush(player.uniqueId)
    }

    /** 접속 시 자동 로드 (RoonSlotRepository.onJoin이 처리하므로 no-op) */
    fun loadSlots(player: Player) = Unit

    /** 퇴장 시 자동 저장 (RoonSlotRepository.onQuit이 처리하므로 no-op) */
    fun removeSlots(player: Player) = Unit

    // ── 합산 스텟 ────────────────────────────────────────────────────────
    fun getTotalStats(player: Player): Map<JewelStatType, Double> {
        val total = mutableMapOf<JewelStatType, Double>()
        getSlots(player).filterNotNull().forEach { jewel ->
            parseStats(jewel).forEach { stat ->
                total[stat.type] = (total[stat.type] ?: 0.0) + stat.value
            }
        }
        return total
    }

    // ── 보석 아이템 생성 (관리자 지급용, 미감정 상태) ───────────────────
    fun createJewel(grade: JewelGrade, amount: Int = 1): ItemStack {
        val mat = when (grade) {
            JewelGrade.LOW     -> Material.QUARTZ
            JewelGrade.MID     -> Material.AMETHYST_SHARD
            JewelGrade.HIGH    -> Material.DIAMOND
            JewelGrade.SUPREME -> Material.NETHER_STAR
        }
        val stack = ItemStack(mat, amount.coerceIn(1, 64))
        val meta: ItemMeta = stack.itemMeta
        meta.setDisplayName("${grade.color}✦ ${grade.displayName} 보석")
        meta.lore = listOf(
            "§r",
            "  §7감정되지 않은 보석입니다.",
            "  §e/감정 §7명령어로 스탯을 부여받으세요.",
            "§r",
            "  §8[미감정 보석]"
        )
        meta.persistentDataContainer.set(keyJewelGrade, PersistentDataType.STRING, grade.id)
        stack.itemMeta = meta
        return stack
    }

    // ── 감정: 랜덤 스텟 부여 ────────────────────────────────────────────
    fun appraise(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        val pdc  = meta.persistentDataContainer
        val gradeId = pdc.get(keyJewelGrade, PersistentDataType.STRING) ?: return false
        val grade   = JewelGrade.fromId(gradeId) ?: return false

        val lineCount = (grade.minLines..grade.maxLines).random()
        val types     = JewelStat.ALL_TYPES.shuffled().take(lineCount)
        val stats     = types.map { JewelStat(it, JewelStat.randomValue(it, grade)) }

        val serialized = stats.joinToString("|") { "${it.type.key}:${it.value}" }
        pdc.set(keyJewelStats,  PersistentDataType.STRING, serialized)
        pdc.set(keyAppraised,   PersistentDataType.BYTE, 1)

        meta.setDisplayName("${grade.color}✦ ${grade.displayName} 보석")
        val lore = mutableListOf(
            "§8──────────────────",
            "  ${grade.color}* §f등급 §8: ${grade.color}${grade.displayName}",
            "§8──────────────────"
        )
        stats.forEach { lore.add(it.toLoreLine()) }
        lore.add("§8──────────────────")
        lore.add("  §8[감정된 보석]")
        meta.lore = lore
        item.itemMeta = meta
        return true
    }

    // ── 보석 여부 확인 ───────────────────────────────────────────────────
    fun isJewel(item: ItemStack?): Boolean {
        val pdc = item?.itemMeta?.persistentDataContainer ?: return false
        return pdc.has(keyJewelGrade, PersistentDataType.STRING)
    }

    fun isAppraised(item: ItemStack?): Boolean {
        val pdc = item?.itemMeta?.persistentDataContainer ?: return false
        return pdc.has(keyAppraised, PersistentDataType.BYTE)
    }

    // ── 스텟 파싱 ───────────────────────────────────────────────────────
    fun parseStats(item: ItemStack): List<JewelStat> {
        val raw = item.itemMeta?.persistentDataContainer
            ?.get(keyJewelStats, PersistentDataType.STRING) ?: return emptyList()
        return raw.split("|").mapNotNull {
            val parts = it.split(":")
            if (parts.size != 2) return@mapNotNull null
            val type  = JewelStatType.fromKey(parts[0]) ?: return@mapNotNull null
            JewelStat(type, parts[1].toDoubleOrNull() ?: 0.0)
        }
    }
}
