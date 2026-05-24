package io.zlero.cRRPGCore

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class JewelManager(private val plugin: CRRPGCorePlugin) {

    private val statsCache = ConcurrentHashMap<UUID, Map<JewelStatType, Double>>()

    // ── NBT 키 ──────────────────────────────────────────────────────────
    val keyJewelGrade    = NamespacedKey(plugin, "jewel_grade")      // 보석 등급 id
    val keyJewelStats    = NamespacedKey(plugin, "jewel_stats")      // 스텟 직렬화 문자열
    val keyAppraised     = NamespacedKey(plugin, "jewel_appraised")  // 감정 여부
    val keyJewelRerollCnt = NamespacedKey(plugin, "jewel_reroll_cnt") // 재감정 횟수
    val keyJewelMaxReroll = NamespacedKey(plugin, "jewel_max_reroll") // 최대 재감정 횟수

    // 룬 슬롯은 RoonSlotRepository가 캐시 및 저장 관리

    fun getSlots(player: Player): Array<ItemStack?> =
        plugin.roonSlotRepository.get(player.uniqueId)?.slots ?: arrayOfNulls(9)

    fun setSlot(player: Player, index: Int, item: ItemStack?) {
        plugin.roonSlotRepository.update(player.uniqueId) {
            slots[index] = item
        }
        statsCache.remove(player.uniqueId)
    }

    /** GUI 닫을 때 즉시 저장 (dirty 데이터를 DB에 flush) */
    fun saveSlots(player: Player) {
        plugin.roonSlotRepository.flush(player.uniqueId)
        statsCache.remove(player.uniqueId)
    }

    /** 접속 시 자동 로드 (RoonSlotRepository.onJoin이 처리하므로 no-op) */
    fun loadSlots(player: Player) = Unit

    /** 퇴장 시 자동 저장 (RoonSlotRepository.onQuit이 처리하므로 no-op) */
    fun removeSlots(player: Player) {
        statsCache.remove(player.uniqueId)
    }

    /** 외부에서 캐시 무효화가 필요할 때 호출 */
    fun invalidateStatsCache(uuid: UUID) {
        statsCache.remove(uuid)
    }

    // ── 합산 스텟 ────────────────────────────────────────────────────────
    fun getTotalStats(player: Player): Map<JewelStatType, Double> {
        val uuid = player.uniqueId
        statsCache[uuid]?.let { return it }
        val total = mutableMapOf<JewelStatType, Double>()
        getSlots(player).filterNotNull().forEach { jewel ->
            parseStats(jewel).forEach { stat ->
                total[stat.type] = (total[stat.type] ?: 0.0) + stat.value
            }
        }
        statsCache[uuid] = total
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

        val stats      = rollJewelStats(grade)
        val serialized = serializeStats(stats)
        pdc.set(keyJewelStats,     PersistentDataType.STRING,  serialized)
        pdc.set(keyAppraised,      PersistentDataType.BYTE,    1)
        pdc.set(keyJewelRerollCnt, PersistentDataType.INTEGER, 0)
        if (!pdc.has(keyJewelMaxReroll, PersistentDataType.INTEGER)) {
            pdc.set(keyJewelMaxReroll, PersistentDataType.INTEGER,
                plugin.appraisalManager.defaultMaxJewelReroll)
        }
        // remaining 계산 후 한 번에 lore까지 구성 — itemMeta set을 1회로 통합
        val maxReroll = pdc.get(keyJewelMaxReroll, PersistentDataType.INTEGER)
            ?: plugin.appraisalManager.defaultMaxJewelReroll
        val remaining = if (maxReroll == -1) -1 else maxReroll
        rebuildJewelLore(meta, grade, stats, remaining)
        item.itemMeta = meta
        return true
    }

    // ── 재감정: 스텟 재롤 ───────────────────────────────────────────────
    fun reappraise(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        val pdc  = meta.persistentDataContainer
        val gradeId = pdc.get(keyJewelGrade, PersistentDataType.STRING) ?: return false
        val grade   = JewelGrade.fromId(gradeId) ?: return false

        val cur = pdc.get(keyJewelRerollCnt, PersistentDataType.INTEGER) ?: 0
        val max = pdc.get(keyJewelMaxReroll,  PersistentDataType.INTEGER)
            ?: plugin.appraisalManager.defaultMaxJewelReroll
        if (max != -1 && cur >= max) return false

        val stats      = rollJewelStats(grade)
        val serialized = serializeStats(stats)
        pdc.set(keyJewelStats,     PersistentDataType.STRING,  serialized)
        pdc.set(keyJewelRerollCnt, PersistentDataType.INTEGER, cur + 1)
        val remaining = if (max == -1) -1 else (max - cur - 1).coerceAtLeast(0)
        rebuildJewelLore(meta, grade, stats, remaining)
        item.itemMeta = meta
        return true
    }

    // ── 스텟 생성 공통 헬퍼 ──────────────────────────────────────────────
    private fun rollJewelStats(grade: JewelGrade): List<JewelStat> {
        val lineCount = (grade.minLines..grade.maxLines).random()
        val types     = JewelStat.ALL_TYPES.shuffled().take(lineCount)
        return types.map { JewelStat(it, JewelStat.randomValue(it, grade)) }
    }

    private fun serializeStats(stats: List<JewelStat>): String =
        stats.joinToString("|") { "${it.type.key}:${it.value}" }

    // ── 남은 재감정 횟수 ─────────────────────────────────────────────────
    fun getJewelRerollRemaining(item: ItemStack): Int {
        val pdc = item.itemMeta?.persistentDataContainer ?: return 0
        val cur = pdc.get(keyJewelRerollCnt, PersistentDataType.INTEGER) ?: 0
        val max = pdc.get(keyJewelMaxReroll,  PersistentDataType.INTEGER)
            ?: plugin.appraisalManager.defaultMaxJewelReroll
        return if (max == -1) -1 else (max - cur).coerceAtLeast(0)
    }

    fun isJewelRerollMaxReached(item: ItemStack): Boolean {
        val rem = getJewelRerollRemaining(item)
        return rem == 0
    }

    // ── 로어 재구성 (meta에 직접 기록 — 호출 측에서 item.itemMeta = meta 1회만 수행) ──────────────
    private fun rebuildJewelLore(meta: org.bukkit.inventory.meta.ItemMeta, grade: JewelGrade,
                                  stats: List<JewelStat>, remaining: Int) {
        meta.setDisplayName("${grade.color}✦ ${grade.displayName} 보석")
        val remainStr = if (remaining < 0) "∞" else "${remaining}회"
        val lore = mutableListOf(
            "§8──────────────────",
            "  ${grade.color}* §f등급 §8: ${grade.color}${grade.displayName}",
            "§8──────────────────"
        )
        stats.forEach { lore.add(it.toLoreLine()) }
        lore.add("§8──────────────────")
        lore.add("  §7재감정 §8: §e${remainStr} 남음")
        lore.add("  §8[감정된 보석]")
        meta.lore = lore
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
