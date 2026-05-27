package io.zlero.cRRPGCore.db

import io.zlero.cRRPGCore.CRRPGCorePlugin
import io.zlero.cRFramework.core.component.annotation.Module
import io.zlero.cRFramework.core.component.annotation.Setup
import io.zlero.cRFramework.core.component.annotation.Teardown
import io.zlero.cRFramework.database.DatabaseConfig
import io.zlero.cRFramework.database.DatabaseType
import io.zlero.cRFramework.database.datasource.DataSourceFactory
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * CRRPGCore 데이터베이스 모듈
 *
 * DatabaseModule 은 final 클래스이므로 상속 불가 — DataSourceFactory 를 직접 호출하는
 * 독립 @Module 로 구현합니다.
 *
 * config.yml 의 storage.type 값에 따라 SQLite / MySQL 을 자동으로 선택합니다.
 *
 *   storage:
 *     type: sqlite    # sqlite (기본) 또는 mysql
 *     mysql:
 *       host:      localhost
 *       port:      3306
 *       database:  minecraft
 *       username:  root
 *       password:  ""
 *       pool-size: 5
 *
 * yaml 타입은 더 이상 지원되지 않습니다. sqlite 로 대체되며,
 * /rpgcore migrate 명령어로 PlayerData.yml → DB 이전을 진행하세요.
 *
 * Lifecycle:
 *   @Setup    — DB 연결 + 테이블 자동 생성 (scan() 단계에서 실행)
 *   @Teardown — 전체 레포지토리 saveAll() + DB 연결 종료 (onDisable 단계)
 */
@Module
class CRRPGDatabaseModule(private val plugin: JavaPlugin) {

    @Setup
    fun onSetup() {
        val config = buildConfig(plugin)
        DataSourceFactory.connect(config, plugin.dataFolder)

        try {
            transaction {
                SchemaUtils.createMissingTablesAndColumns(PlayerDataTable, RoonSlotTable)
            }
            plugin.logger.info("[CRRPGCore/DB] ${config.type} 연결 완료, 테이블 초기화")
        } catch (e: Exception) {
            plugin.logger.severe("[CRRPGCore/DB] 테이블 생성 실패: ${e.message}")
            e.printStackTrace()
        }
    }

    /** 플러그인 enable 시점에 한번 더 테이블 존재를 보장하는 보조 메서드 */
    fun ensureTables() {
        try {
            transaction {
                SchemaUtils.createMissingTablesAndColumns(PlayerDataTable, RoonSlotTable)
            }
        } catch (e: Exception) {
            plugin.logger.severe("[CRRPGCore/DB] ensureTables 실패: ${e.message}")
            e.printStackTrace()
        }
    }

    @Teardown
    fun onTeardown() {
        // CRRPGCorePlugin 의 lateinit 레포지토리에 직접 접근하여 saveAll
        val rpg = plugin as? CRRPGCorePlugin
        try {
            rpg?.playerDataRepository?.saveAll()
            rpg?.roonSlotRepository?.saveAll()
        } catch (e: Exception) {
            plugin.logger.warning("[CRRPGCore/DB] 데이터 저장 중 오류: ${e.message}")
        }
        DataSourceFactory.close()
        plugin.logger.info("[CRRPGCore/DB] 데이터베이스 연결 종료")
    }

    companion object {
        fun buildConfig(plugin: JavaPlugin): DatabaseConfig {
            val cfg  = plugin.config
            val type = cfg.getString("storage.type", "sqlite")
                ?.lowercase()?.trim() ?: "sqlite"

            if (type == "yaml") {
                plugin.logger.warning("[CRRPGCore] storage.type=yaml 은 더 이상 지원되지 않습니다. SQLite 로 대체됩니다.")
                plugin.logger.warning("[CRRPGCore] 기존 YAML 데이터 이전: /rpgcore migrate 를 사용하세요.")
            }

            return when (type) {
                "mysql" -> {
                    plugin.logger.info("[CRRPGCore] 저장소 백엔드: MySQL")
                    DatabaseConfig(
                        type     = DatabaseType.MYSQL,
                        host     = cfg.getString("storage.mysql.host",     "localhost")!!,
                        port     = cfg.getInt   ("storage.mysql.port",     3306),
                        database = cfg.getString("storage.mysql.database", "minecraft")!!,
                        username = cfg.getString("storage.mysql.username", "root")!!,
                        password = cfg.getString("storage.mysql.password", "")!!,
                        poolSize = cfg.getInt   ("storage.mysql.pool-size", 5)
                    )
                }
                else -> {
                    // sqlite (기본) 또는 yaml fallback
                    if (type != "yaml") plugin.logger.info("[CRRPGCore] 저장소 백엔드: SQLite")
                    DatabaseConfig()
                }
            }
        }
    }
}
