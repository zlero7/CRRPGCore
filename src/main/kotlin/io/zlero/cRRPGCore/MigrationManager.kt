package io.zlero.cRRPGCore

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID

/**
 * YAML → SQLite / MySQL 데이터 마이그레이션 매니저
 *
 * 구 버전에서 저장된 PlayerData.yml 파일을 읽어 현재 DB(SQLite 또는 MySQL)에 삽입합니다.
 * 이미 DB에 존재하는 UUID는 덮어쓰지 않고 스킵합니다.
 *
 * 구 PlayerData.yml 구조:
 *   players:
 *     {uuid}:
 *       level: 1
 *       xp: 0
 *       stat-points: 0
 *       strength: 0
 *       vitality: 0
 *       agility: 0
 *   roon:
 *     {uuid}:
 *       0: "<Base64>"
 *       1: "<Base64>"
 *       ...
 *       8: "<Base64>"
 */
class MigrationManager(private val plugin: CRRPGCorePlugin) {

    data class MigrationResult(
        val playerMigrated : Int,
        val playerSkipped  : Int,
        val roonMigrated   : Int,
        val errors         : Int
    ) {
        val hasErrors get() = errors > 0
    }

    /**
     * PlayerData.yml 파일에서 데이터를 읽어 현재 DB로 이전합니다.
     * 서버 메인 스레드에서 호출하되, 비동기 처리가 필요하면 호출부에서 처리하세요.
     */
    fun migrateFromYaml(): MigrationResult {
        val file = File(plugin.dataFolder, "PlayerData.yml")

        if (!file.exists()) {
            plugin.logger.info("[CRRPGCore/Migration] PlayerData.yml 파일이 없습니다. 마이그레이션을 건너뜁니다.")
            return MigrationResult(0, 0, 0, 0)
        }

        plugin.logger.info("[CRRPGCore/Migration] PlayerData.yml 로드 중...")
        val yaml = YamlConfiguration.loadConfiguration(file)

        var playerMigrated = 0
        var playerSkipped  = 0
        var roonMigrated   = 0
        var errors         = 0

        // ── 플레이어 데이터 ──────────────────────────────────────────────────
        val playersSection = yaml.getConfigurationSection("players")
        if (playersSection != null) {
            for (uuidStr in playersSection.getKeys(false)) {
                val uuid = runCatching { UUID.fromString(uuidStr) }.getOrElse {
                    plugin.logger.warning("[CRRPGCore/Migration] UUID 파싱 실패: $uuidStr")
                    errors++
                    continue
                }
                val p = "players.$uuidStr"
                val data = PlayerData(
                    level      = yaml.getInt ("$p.level",       1),
                    xp         = yaml.getLong("$p.xp",         0L),
                    statPoints = yaml.getInt ("$p.stat-points", 0),
                    strength   = yaml.getInt ("$p.strength",    0),
                    vitality   = yaml.getInt ("$p.vitality",    0),
                    agility    = yaml.getInt ("$p.agility",     0)
                )
                runCatching {
                    if (plugin.playerDataRepository.migrateInsert(uuid, data))
                        playerMigrated++ else playerSkipped++
                }.onFailure {
                    plugin.logger.warning("[CRRPGCore/Migration] 플레이어 $uuidStr 저장 실패: ${it.message}")
                    errors++
                }
            }
        }

        // ── 룬 슬롯 데이터 ──────────────────────────────────────────────────
        val roonSection = yaml.getConfigurationSection("roon")
        if (roonSection != null) {
            for (uuidStr in roonSection.getKeys(false)) {
                val uuid = runCatching { UUID.fromString(uuidStr) }.getOrElse {
                    errors++
                    continue
                }
                val slots = Array(9) { i ->
                    yaml.getString("roon.$uuidStr.$i")
                        ?.let { plugin.roonSlotRepository.decodeItem(it) }
                }
                runCatching {
                    if (plugin.roonSlotRepository.migrateInsert(uuid, RoonSlotData(slots)))
                        roonMigrated++
                }.onFailure {
                    plugin.logger.warning("[CRRPGCore/Migration] 룬 $uuidStr 저장 실패: ${it.message}")
                    errors++
                }
            }
        }

        plugin.logger.info(
            "[CRRPGCore/Migration] 완료 — " +
            "플레이어: ${playerMigrated}명 이전 / ${playerSkipped}명 스킵 | " +
            "룬: ${roonMigrated}명 이전 | 오류: ${errors}건"
        )
        return MigrationResult(playerMigrated, playerSkipped, roonMigrated, errors)
    }
}
