package io.zlero.cRRPGCore

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import java.util.UUID

// ═══════════════════════════════════════════════════════════════════════════
//  PlayerDataManager
//  config.yml  storage.type 에 따라 YAML / MySQL 구현체를 자동 선택
//
//  storage:
//    type: yaml        # yaml 또는 mysql
//    mysql:
//      host:     localhost
//      port:     3306
//      database: minecraft
//      username: root
//      password: ""
//      table-prefix:        crrpg_
//      pool-size:           5
//      connection-timeout:  30000
// ═══════════════════════════════════════════════════════════════════════════
class PlayerDataManager(private val plugin: CRRPGCorePlugin) {

    private lateinit var backend: Backend
    private var isMysql = false

    // ── 초기화 ───────────────────────────────────────────────────────────

    fun load() {
        val type = plugin.config.getString("storage.type", "yaml")
            ?.lowercase()?.trim() ?: "yaml"

        isMysql = type == "mysql"
        backend = if (isMysql) {
            plugin.logger.info("[CRRPGCore] 데이터 저장소: MySQL")
            MysqlBackend(plugin).also { it.init() }
        } else {
            plugin.logger.info("[CRRPGCore] 데이터 저장소: YAML")
            YamlBackend(plugin).also { it.init() }
        }
    }

    /** 플러그인 disable 시 호출 — MySQL 연결 풀 정상 종료 */
    fun close() = backend.close()

    // ── 공개 API ─────────────────────────────────────────────────────────

    fun hasPlayer(uuid: UUID): Boolean                      = backend.hasPlayer(uuid)
    fun loadPlayer(uuid: UUID): PlayerData                  = backend.loadPlayer(uuid)
    fun savePlayer(uuid: UUID, data: PlayerData)            = backend.savePlayer(uuid, data)
    fun saveRoonSlots(uuid: UUID, slots: Array<ItemStack?>) = backend.saveRoonSlots(uuid, slots)
    fun loadRoonSlots(uuid: UUID): Array<ItemStack?>        = backend.loadRoonSlots(uuid)

    fun saveAll(dataMap: Map<UUID, PlayerData>) {
        dataMap.forEach { (uuid, data) -> savePlayer(uuid, data) }
    }

    fun savePlayerAsync(uuid: UUID, data: PlayerData) {
        if (isMysql) {
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                savePlayer(uuid, data)
            })
        } else {
            savePlayer(uuid, data)
        }
    }

    fun saveRoonSlotsAsync(uuid: UUID, slots: Array<ItemStack?>) {
        if (isMysql) {
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                saveRoonSlots(uuid, slots)
            })
        } else {
            saveRoonSlots(uuid, slots)
        }
    }

    fun saveAllAsync(dataMap: Map<UUID, PlayerData>) {
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            dataMap.forEach { (uuid, data) -> savePlayer(uuid, data) }
        })
    }



    // ══════════════════════════════════════════════════════════════════════
    //  공통 인터페이스
    // ══════════════════════════════════════════════════════════════════════
    private interface Backend {
        fun init()
        fun close() {}
        fun hasPlayer(uuid: UUID): Boolean
        fun loadPlayer(uuid: UUID): PlayerData
        fun savePlayer(uuid: UUID, data: PlayerData)
        fun saveRoonSlots(uuid: UUID, slots: Array<ItemStack?>)
        fun loadRoonSlots(uuid: UUID): Array<ItemStack?>
    }

    // ══════════════════════════════════════════════════════════════════════
    //  YAML 구현체
    // ══════════════════════════════════════════════════════════════════════
    private inner class YamlBackend(private val plugin: CRRPGCorePlugin) : Backend {

        private val file = File(plugin.dataFolder, "PlayerData.yml")
        private lateinit var yaml: YamlConfiguration

        override fun init() {
            if (!file.exists()) { file.parentFile.mkdirs(); file.createNewFile() }
            yaml = YamlConfiguration.loadConfiguration(file)
        }

        override fun hasPlayer(uuid: UUID) = yaml.contains("players.$uuid")

        override fun loadPlayer(uuid: UUID): PlayerData {
            val p = "players.$uuid"
            if (!yaml.contains(p)) return PlayerData()
            return PlayerData(
                level      = yaml.getInt("$p.level",       1),
                xp         = yaml.getLong("$p.xp",         0L),
                statPoints = yaml.getInt("$p.stat-points", 0),
                strength   = yaml.getInt("$p.strength",    0),
                vitality   = yaml.getInt("$p.vitality",    0),
                agility    = yaml.getInt("$p.agility",     0)
            )
        }

        override fun savePlayer(uuid: UUID, data: PlayerData) {
            val p = "players.$uuid"
            yaml.set("$p.level",       data.level)
            yaml.set("$p.xp",          data.xp)
            yaml.set("$p.stat-points", data.statPoints)
            yaml.set("$p.strength",    data.strength)
            yaml.set("$p.vitality",    data.vitality)
            yaml.set("$p.agility",     data.agility)
            yaml.save(file)
        }

        override fun saveRoonSlots(uuid: UUID, slots: Array<ItemStack?>) {
            val p = "roon.$uuid"
            for (i in slots.indices) {
                yaml.set("$p.$i", slots[i]?.let { itemToBase64(it) })
            }
            yaml.save(file)
        }

        override fun loadRoonSlots(uuid: UUID): Array<ItemStack?> {
            val p = "roon.$uuid"
            return Array(9) { i ->
                yaml.getString("$p.$i")?.let { itemFromBase64(it) }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MySQL 구현체  (HikariCP 커넥션 풀)
    //
    //  의존: HikariCP JAR을 서버 classpath에 포함해야 합니다.
    //  Spigot/Paper 빌드 시 build.gradle 또는 pom.xml에 추가:
    //    implementation 'com.zaxxer:HikariCP:5.1.0'
    //
    //  테이블:
    //    {prefix}players  — 레벨/스텟
    //    {prefix}roon     — 룬 슬롯 9칸 (Base64)
    // ══════════════════════════════════════════════════════════════════════
    private inner class MysqlBackend(private val plugin: CRRPGCorePlugin) : Backend {

        private lateinit var ds: com.zaxxer.hikari.HikariDataSource

        private val prefix  get() = plugin.config.getString("storage.mysql.table-prefix", "crrpg_")!!
        private val tPlayer get() = "`${prefix}players`"
        private val tRoon   get() = "`${prefix}roon`"

        override fun init() {
            val cfg = plugin.config
            val hc  = com.zaxxer.hikari.HikariConfig()
            hc.jdbcUrl = "jdbc:mysql://" +
                    "${cfg.getString("storage.mysql.host", "localhost")}:" +
                    "${cfg.getInt("storage.mysql.port", 3306)}/" +
                    "${cfg.getString("storage.mysql.database", "minecraft")}" +
                    "?useSSL=false&characterEncoding=UTF-8&serverTimezone=UTC"
            hc.username        = cfg.getString("storage.mysql.username", "root")!!
            hc.password        = cfg.getString("storage.mysql.password", "")!!
            hc.maximumPoolSize = cfg.getInt("storage.mysql.pool-size", 5)
            hc.connectionTimeout = cfg.getLong("storage.mysql.connection-timeout", 30_000L)
            hc.poolName        = "CRRPGCore-Pool"
            hc.addDataSourceProperty("cachePrepStmts",        "true")
            hc.addDataSourceProperty("prepStmtCacheSize",     "250")
            hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            ds = com.zaxxer.hikari.HikariDataSource(hc)
            createTables()
        }

        override fun close() {
            if (::ds.isInitialized && !ds.isClosed) {
                ds.close()
                plugin.logger.info("[CRRPGCore] MySQL 연결 풀 종료.")
            }
        }

        private fun createTables() {
            ds.connection.use { con ->
                con.createStatement().use { st ->
                    st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS ${tPlayer} (
                            `uuid`        VARCHAR(36) NOT NULL PRIMARY KEY,
                            `level`       INT         NOT NULL DEFAULT 1,
                            `xp`          BIGINT      NOT NULL DEFAULT 0,
                            `stat_points` INT         NOT NULL DEFAULT 0,
                            `strength`    INT         NOT NULL DEFAULT 0,
                            `vitality`    INT         NOT NULL DEFAULT 0,
                            `agility`     INT         NOT NULL DEFAULT 0
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """.trimIndent())
                    st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS ${tRoon} (
                            `uuid`  VARCHAR(36) NOT NULL PRIMARY KEY,
                            `slot0` TEXT, `slot1` TEXT, `slot2` TEXT,
                            `slot3` TEXT, `slot4` TEXT, `slot5` TEXT,
                            `slot6` TEXT, `slot7` TEXT, `slot8` TEXT
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """.trimIndent())
                }
            }
        }

        override fun hasPlayer(uuid: UUID): Boolean {
            ds.connection.use { con ->
                con.prepareStatement("SELECT 1 FROM $tPlayer WHERE `uuid`=? LIMIT 1").use { ps ->
                    ps.setString(1, uuid.toString())
                    return ps.executeQuery().next()
                }
            }
        }

        override fun loadPlayer(uuid: UUID): PlayerData {
            ds.connection.use { con ->
                con.prepareStatement(
                    "SELECT `level`,`xp`,`stat_points`,`strength`,`vitality`,`agility` FROM $tPlayer WHERE `uuid`=?"
                ).use { ps ->
                    ps.setString(1, uuid.toString())
                    val rs = ps.executeQuery()
                    if (!rs.next()) return PlayerData()
                    return PlayerData(
                        level      = rs.getInt("level"),
                        xp         = rs.getLong("xp"),
                        statPoints = rs.getInt("stat_points"),
                        strength   = rs.getInt("strength"),
                        vitality   = rs.getInt("vitality"),
                        agility    = rs.getInt("agility")
                    )
                }
            }
        }

        override fun savePlayer(uuid: UUID, data: PlayerData) {
            ds.connection.use { con ->
                con.prepareStatement("""
                    INSERT INTO $tPlayer (`uuid`,`level`,`xp`,`stat_points`,`strength`,`vitality`,`agility`)
                    VALUES (?,?,?,?,?,?,?)
                    ON DUPLICATE KEY UPDATE
                      `level`=VALUES(`level`), `xp`=VALUES(`xp`),
                      `stat_points`=VALUES(`stat_points`),
                      `strength`=VALUES(`strength`),
                      `vitality`=VALUES(`vitality`),
                      `agility`=VALUES(`agility`)
                """.trimIndent()).use { ps ->
                    ps.setString(1, uuid.toString())
                    ps.setInt(2, data.level)
                    ps.setLong(3, data.xp)
                    ps.setInt(4, data.statPoints)
                    ps.setInt(5, data.strength)
                    ps.setInt(6, data.vitality)
                    ps.setInt(7, data.agility)
                    ps.executeUpdate()
                }
            }
        }

        override fun saveRoonSlots(uuid: UUID, slots: Array<ItemStack?>) {
            ds.connection.use { con ->
                con.prepareStatement("""
                    INSERT INTO $tRoon
                      (`uuid`,`slot0`,`slot1`,`slot2`,`slot3`,`slot4`,`slot5`,`slot6`,`slot7`,`slot8`)
                    VALUES (?,?,?,?,?,?,?,?,?,?)
                    ON DUPLICATE KEY UPDATE
                      `slot0`=VALUES(`slot0`), `slot1`=VALUES(`slot1`), `slot2`=VALUES(`slot2`),
                      `slot3`=VALUES(`slot3`), `slot4`=VALUES(`slot4`), `slot5`=VALUES(`slot5`),
                      `slot6`=VALUES(`slot6`), `slot7`=VALUES(`slot7`), `slot8`=VALUES(`slot8`)
                """.trimIndent()).use { ps ->
                    ps.setString(1, uuid.toString())
                    for (i in 0..8) ps.setString(i + 2, slots.getOrNull(i)?.let { itemToBase64(it) })
                    ps.executeUpdate()
                }
            }
        }

        override fun loadRoonSlots(uuid: UUID): Array<ItemStack?> {
            ds.connection.use { con ->
                con.prepareStatement(
                    "SELECT `slot0`,`slot1`,`slot2`,`slot3`,`slot4`,`slot5`,`slot6`,`slot7`,`slot8` FROM $tRoon WHERE `uuid`=?"
                ).use { ps ->
                    ps.setString(1, uuid.toString())
                    val rs = ps.executeQuery()
                    if (!rs.next()) return arrayOfNulls(9)
                    return Array(9) { i -> rs.getString("slot$i")?.let { itemFromBase64(it) } }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  공통 Base64 직렬화 유틸
    // ══════════════════════════════════════════════════════════════════════
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
}