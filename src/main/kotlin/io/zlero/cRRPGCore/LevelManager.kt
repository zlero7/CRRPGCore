package io.zlero.cRRPGCore

import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.util.Ticks
import org.bukkit.Sound
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import java.util.UUID

class LevelManager(private val plugin: CRRPGCorePlugin) {

    var maxLevel: Int    = 100
        private set
    var baseXp: Int      = 100
        private set
    var multiplier: Double = 1.2
        private set

    private var titleLevelUp: String     = "§a✦ 레벨업! ✦"
    private var subtitleLevelUp: String  = "§e현재 레벨 : {level}"
    private var titleMaxLevel: String    = "§6★ 최대 레벨 달성! ★"
    private var subtitleMaxLevel: String = "§eLv.{level} MAX"
    private var titleFadeIn: Int         = 10
    private var titleStay: Int           = 60
    private var titleFadeOut: Int        = 20

    private var msgXpGained: String  = "§7[§b+{xp} XP§7] §8(§7{current}§8/§7{required}§8)"
    private var showXpOnGain: Boolean = true

    // ─── Config 로드 ──────────────────────────────────────────────────────
    fun loadConfig(config: FileConfiguration) {
        maxLevel   = config.getInt("level.max-level", 100).coerceAtLeast(1)
        baseXp     = config.getInt("level.base-xp", 100).coerceAtLeast(1)
        multiplier = config.getDouble("level.multiplier", 1.2).coerceAtLeast(1.0)

        titleLevelUp     = config.getString("title.level-up.title",     titleLevelUp)     ?: titleLevelUp
        subtitleLevelUp  = config.getString("title.level-up.subtitle",  subtitleLevelUp)  ?: subtitleLevelUp
        titleMaxLevel    = config.getString("title.max-level.title",    titleMaxLevel)    ?: titleMaxLevel
        subtitleMaxLevel = config.getString("title.max-level.subtitle", subtitleMaxLevel) ?: subtitleMaxLevel
        titleFadeIn      = config.getInt("title.fade-in",  10)
        titleStay        = config.getInt("title.stay",     60)
        titleFadeOut     = config.getInt("title.fade-out", 20)

        msgXpGained  = config.getString("messages.xp-gained", msgXpGained) ?: msgXpGained
        showXpOnGain = config.getBoolean("messages.show-xp-on-gain", true)
    }

    // ─── 경험치 계산 (Long 반환 - 오버플로우 방지) ───────────────────────
    fun getRequiredXpForLevel(level: Int): Long {
        if (level >= maxLevel) return 0L
        return (baseXp * Math.pow(multiplier, level.toDouble())).toLong().coerceAtLeast(1L)
    }

    // ─── 플레이어 데이터 접근 (PlayerRepository 캐시 사용) ───────────────
    fun getPlayerData(player: Player): PlayerData =
        plugin.playerDataRepository.get(player.uniqueId) ?: PlayerData()

    fun savePlayerData(player: Player, level: Int, xp: Long) {
        plugin.playerDataRepository.update(player.uniqueId) {
            this.level = level
            this.xp    = xp
        }
        updateExpBar(player, getPlayerData(player))
    }

    fun removePlayerData(player: Player) {
        // PlayerRepository.onQuit() 에서 자동 저장 처리
        // 명시적 flush가 필요한 경우 호출
        plugin.playerDataRepository.flush(player.uniqueId)
    }

    // ─── 경험치 지급 ──────────────────────────────────────────────────────
    fun giveXp(player: Player, xpAmount: Long) {
        if (xpAmount <= 0L) return

        val data = getPlayerData(player)
        if (data.level >= maxLevel) return

        // 부스트 배수 적용
        val boostMultiplier = plugin.xpBoostManager.getTotalMultiplier(player.uniqueId)
        val boostedXp       = (xpAmount * boostMultiplier).toLong().coerceAtLeast(xpAmount)

        var currentXp    = data.xp + boostedXp
        var currentLevel = data.level
        var leveledUp    = false
        var reachedMax   = false

        val previousLevel = currentLevel

        while (currentLevel < maxLevel) {
            val requiredXp = getRequiredXpForLevel(currentLevel)
            if (currentXp < requiredXp) break
            currentXp    -= requiredXp
            currentLevel++
            leveledUp     = true

            plugin.statManager.grantPoints(player)

            if (currentLevel >= maxLevel) {
                reachedMax = true
                currentXp  = 0L
                break
            }
        }

        if (leveledUp) {
            if (reachedMax) {
                sendTitle(player, titleMaxLevel, subtitleMaxLevel.replace("{level}", currentLevel.toString()))
            } else {
                sendTitle(player, titleLevelUp, subtitleLevelUp.replace("{level}", currentLevel.toString()))
            }
            player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f)
            val event = PlayerLevelUpEvent(player, previousLevel, currentLevel, reachedMax)
            org.bukkit.Bukkit.getPluginManager().callEvent(event)
        }

        if (showXpOnGain && !leveledUp) {
            val required = getRequiredXpForLevel(currentLevel)
            val boostTag = if (boostMultiplier > 1.0) " §6(§e${boostMultiplier}x 부스트§6)" else ""
            player.sendMessage(
                msgXpGained
                    .replace("{xp}",       boostedXp.toString())
                    .replace("{current}",  currentXp.toString())
                    .replace("{required}", required.toString()) + boostTag
            )
        }

        savePlayerData(player, currentLevel, currentXp)
    }

    // Int로 들어오는 바닐라 경험치 이벤트 호환용 오버로드
    fun giveXp(player: Player, xpAmount: Int) = giveXp(player, xpAmount.toLong())

    // ─── 타이틀 전송 ─────────────────────────────────────────────────────
    private fun sendTitle(player: Player, titleStr: String, subtitleStr: String) {
        val times = Title.Times.times(
            Ticks.duration(titleFadeIn.toLong()),
            Ticks.duration(titleStay.toLong()),
            Ticks.duration(titleFadeOut.toLong())
        )
        player.showTitle(
            Title.title(
                Component.text(titleStr),
                Component.text(subtitleStr),
                times
            )
        )
    }

    // ─── EXP 바 업데이트 ─────────────────────────────────────────────────
    private fun updateExpBar(player: Player, data: PlayerData) {
        player.level = data.level
        val required = getRequiredXpForLevel(data.level)
        player.setExp(
            if (required <= 0L || data.level >= maxLevel) 1f
            else (data.xp.toFloat() / required.toFloat()).coerceIn(0f, 1f)
        )
    }

    // ─── 관리자용 유틸 ───────────────────────────────────────────────────
    fun setLevel(player: Player, level: Int) {
        savePlayerData(player, level.coerceIn(1, maxLevel), 0L)
    }

    fun setXp(player: Player, xp: Long) {
        val data = getPlayerData(player)
        savePlayerData(player, data.level, xp.coerceAtLeast(0L))
    }
}
