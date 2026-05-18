package io.zlero.cRRPGCore

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

class JewelManager(private val plugin: CRRPGCorePlugin) {

    // ── NBT 키 ──────────────────────────────────────────────────────────
    val keyJewelGrade  = NamespacedKey(plugin, "jewel_grade")      // 보석 등급 id
    val keyJewelStats  = NamespacedKey(plugin, "jewel_stats")      // 스텟 직렬화 문자열
    val keyAppraised   = NamespacedKey(plugin, "jewel_appraised")  // 감정 여부

    // ── 룬 슬롯 저장 (서버 메모리, 로그아웃 시 파일 저장) ──────────────
    // key: UUID, value: 9슬롯 아이템 직렬화 목록 (null = 빈 슬롯)
    private val roonSlots = HashMap<UUID, Array<ItemStack?>>()

    fun getSlots(player: Player): Array<ItemStack?> =
        roonSlots.getOrPut(player.uniqueId) {
            plugin.playerDataManager.loadRoonSlots(player.uniqueId)
        }

    fun setSlot(player: Player, index: Int, item: ItemStack?) {
        val slots = getSlots(player)
        slots[index] = item
    }

    fun saveSlots(player: Player) {
        plugin.playerDataManager.saveRoonSlots(player.uniqueId, getSlots(player))
    }

    fun loadSlots(player: Player) {
        roonSlots[player.uniqueId] = plugin.playerDataManager.loadRoonSlots(player.uniqueId)
    }

    fun removeSlots(player: Player) {
        saveSlots(player)
        roonSlots.remove(player.uniqueId)
    }

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
        meta.setDisplayName("${grade.color}\u2726 ${grade.displayName} \ubcf4\uc11d")
        meta.lore = listOf(
            "\u00a7r",
            "  \u00a77\uac10\uc815\ub418\uc9c0 \uc54a\uc740 \ubcf4\uc11d\uc785\ub2c8\ub2e4.",
            "  \u00a7e/\uac10\uc815 \u00a77\uba85\ub839\uc5b4\ub85c \uc2a4\ud0ef\uc744 \ubd80\uc5ec\ubc1b\uc73c\uc138\uc694.",
            "\u00a7r",
            "  \u00a78[\ubbf8\uac10\uc815 \ubcf4\uc11d]"
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


        // 직렬화: "key:value|key:value"
        val serialized = stats.joinToString("|") { "${it.type.key}:${it.value}" }
        pdc.set(keyJewelStats,  PersistentDataType.STRING, serialized)
        pdc.set(keyAppraised,   PersistentDataType.BYTE, 1)

        meta.setDisplayName("${grade.color}\u2726 ${grade.displayName} \ubcf4\uc11d")
        val lore = mutableListOf(
            "\u00a78\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500",
            "  ${grade.color}* \u00a7f\ub4f1\uae09 \u00a78: ${grade.color}${grade.displayName}",
            "\u00a78\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
        )
        stats.forEach { lore.add(it.toLoreLine()) }
        lore.add("\u00a78\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500")
        lore.add("  \u00a78[\uac10\uc815\ub41c \ubcf4\uc11d]")
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