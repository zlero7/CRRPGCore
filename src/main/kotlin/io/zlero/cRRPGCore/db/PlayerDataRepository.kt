package io.zlero.cRRPGCore.db

import io.zlero.cRFramework.database.repository.PlayerRepository
import io.zlero.cRRPGCore.model.PlayerData
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 플레이어 레벨/스텟 레포지토리
 *
 * - 접속 시 자동 로드, 퇴장 시 dirty 데이터만 자동 저장 (CRFramework PlayerRepository)
 * - consumeIsNew(uuid): 신규 플레이어 최초 접속 감지 (onPlayerJoin에서 사용)
 */
class PlayerDataRepository : PlayerRepository<PlayerData, PlayerDataTable>(PlayerDataTable) {

    // 신규 플레이어 UUID 추적 — createDefault() 호출 시 추가, consumeIsNew() 호출 시 제거
    private val newPlayers: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    override fun createDefault(uuid: UUID): PlayerData {
        newPlayers.add(uuid)
        return PlayerData()
    }

    override fun load(uuid: UUID): PlayerData? = query {
        PlayerDataTable
            .select { PlayerDataTable.uuid eq uuid.toString() }
            .firstOrNull()
            ?.let { row ->
                PlayerData(
                    level      = row[PlayerDataTable.level],
                    xp         = row[PlayerDataTable.xp],
                    statPoints = row[PlayerDataTable.statPoints],
                    strength   = row[PlayerDataTable.strength],
                    vitality   = row[PlayerDataTable.vitality],
                    agility    = row[PlayerDataTable.agility]
                )
            }
    }

    override fun save(uuid: UUID, data: PlayerData): Unit = query {
        // UPDATE 먼저 시도 → 0행이면 신규 플레이어이므로 INSERT (SELECT 왕복 제거)
        val updated = PlayerDataTable.update({ PlayerDataTable.uuid eq uuid.toString() }) {
            it[level]      = data.level
            it[xp]         = data.xp
            it[statPoints] = data.statPoints
            it[strength]   = data.strength
            it[vitality]   = data.vitality
            it[agility]    = data.agility
        }
        if (updated == 0) {
            PlayerDataTable.insert {
                it[PlayerDataTable.uuid]       = uuid.toString()
                it[PlayerDataTable.level]      = data.level
                it[PlayerDataTable.xp]         = data.xp
                it[PlayerDataTable.statPoints] = data.statPoints
                it[PlayerDataTable.strength]   = data.strength
                it[PlayerDataTable.vitality]   = data.vitality
                it[PlayerDataTable.agility]    = data.agility
            }
        }
    }

    /**
     * 신규 플레이어 여부 확인 (한 번 소비 후 제거)
     * LevelListener.onPlayerJoin(HIGH priority)에서 호출
     */
    fun consumeIsNew(uuid: UUID): Boolean = newPlayers.remove(uuid)

    /**
     * YAML 마이그레이션 전용 — DB에 직접 삽입 (이미 존재하면 스킵)
     * @return true = 삽입됨, false = 이미 존재하여 스킵
     */
    fun migrateInsert(uuid: UUID, data: PlayerData): Boolean = query {
        val exists = PlayerDataTable
            .select { PlayerDataTable.uuid eq uuid.toString() }
            .count() > 0
        if (exists) return@query false

        PlayerDataTable.insert {
            it[PlayerDataTable.uuid]       = uuid.toString()
            it[PlayerDataTable.level]      = data.level
            it[PlayerDataTable.xp]         = data.xp
            it[PlayerDataTable.statPoints] = data.statPoints
            it[PlayerDataTable.strength]   = data.strength
            it[PlayerDataTable.vitality]   = data.vitality
            it[PlayerDataTable.agility]    = data.agility
        }
        true
    }
}
