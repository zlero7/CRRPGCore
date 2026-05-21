package io.zlero.cRRPGCore

import io.zlero.cRFramework.database.repository.PlayerRepository
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.UUID

/**
 * 룬(보석) 슬롯 9칸 데이터 클래스
 * ItemStack?[] 배열을 하나의 객체로 래핑하여 PlayerRepository 캐시에 저장
 */
data class RoonSlotData(var slots: Array<ItemStack?> = arrayOfNulls(9)) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RoonSlotData) return false
        return slots.contentDeepEquals(other.slots)
    }
    override fun hashCode(): Int = slots.contentDeepHashCode()
}

/**
 * 룬 슬롯 레포지토리
 *
 * - 접속 시 자동 로드, 퇴장 시 dirty 데이터만 자동 저장 (CRFramework PlayerRepository)
 * - ItemStack은 BukkitObjectOutputStream으로 Base64 직렬화 → 단일 text 컬럼에 "|" 구분
 */
class RoonSlotRepository(private val plugin: JavaPlugin)
    : PlayerRepository<RoonSlotData, RoonSlotTable>(RoonSlotTable) {

    override fun createDefault(uuid: UUID) = RoonSlotData()

    override fun load(uuid: UUID): RoonSlotData? = query {
        RoonSlotTable
            .select { RoonSlotTable.uuid eq uuid.toString() }
            .firstOrNull()
            ?.let { row ->
                RoonSlotData(deserializeSlots(row[RoonSlotTable.slots]))
            }
    }

    override fun save(uuid: UUID, data: RoonSlotData): Unit = query {
        val serialized = serializeSlots(data.slots)
        val exists = RoonSlotTable
            .select { RoonSlotTable.uuid eq uuid.toString() }
            .count() > 0

        if (exists) {
            RoonSlotTable.update({ RoonSlotTable.uuid eq uuid.toString() }) {
                it[RoonSlotTable.slots] = serialized
            }
        } else {
            RoonSlotTable.insert {
                it[RoonSlotTable.uuid]  = uuid.toString()
                it[RoonSlotTable.slots] = serialized
            }
        }
    }

    // ── 직렬화: ItemStack?[] → Base64 문자열 ("|" 구분) ────────────────────
    private fun serializeSlots(slots: Array<ItemStack?>): String =
        slots.joinToString("|") { item ->
            if (item == null) "" else itemToBase64(item)
        }

    private fun deserializeSlots(raw: String): Array<ItemStack?> {
        if (raw.isEmpty()) return arrayOfNulls(9)
        val parts = raw.split("|", limit = 9)
        return Array(9) { i ->
            val s = parts.getOrElse(i) { "" }
            if (s.isEmpty()) null else itemFromBase64(s)
        }
    }

    private fun itemToBase64(item: ItemStack): String {
        val baos = ByteArrayOutputStream()
        BukkitObjectOutputStream(baos).use { it.writeObject(item) }
        return Base64.getEncoder().encodeToString(baos.toByteArray())
    }

    private fun itemFromBase64(data: String): ItemStack? = try {
        val bais = ByteArrayInputStream(Base64.getDecoder().decode(data))
        BukkitObjectInputStream(bais).use { it.readObject() as? ItemStack }
    } catch (e: Exception) {
        plugin.logger.warning("[CRRPGCore] 룬 슬롯 역직렬화 실패: ${e.message}")
        null
    }

    /** YAML 마이그레이션용 — Base64 문자열을 ItemStack으로 디코드 */
    fun decodeItem(base64: String): ItemStack? = itemFromBase64(base64)

    /**
     * YAML 마이그레이션 전용 — DB에 직접 삽입 (이미 존재하면 스킵)
     * @return true = 삽입됨, false = 이미 존재하여 스킵
     */
    fun migrateInsert(uuid: UUID, data: RoonSlotData): Boolean = query {
        val exists = RoonSlotTable
            .select { RoonSlotTable.uuid eq uuid.toString() }
            .count() > 0
        if (exists) return@query false

        RoonSlotTable.insert {
            it[RoonSlotTable.uuid]  = uuid.toString()
            it[RoonSlotTable.slots] = serializeSlots(data.slots)
        }
        true
    }
}
