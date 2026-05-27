package io.zlero.cRRPGCore

import io.zlero.cRRPGCore.model.StatType
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

/**
 * CRRPGCore 외부 연동 API
 *
 * ─ 다른 플러그인 사용 예시 ─────────────────────────────────────────────
 *
 *   val api = CRRPGCoreAPI.getInstance() ?: return  // 미설치 시 null
 *
 *   // 레벨 / 경험치
 *   val level    = api.getLevel(player)
 *   val xp       = api.getCurrentXp(player)
 *   val required = api.getRequiredXp(player)
 *   api.giveXp(player, 500)
 *
 *   // 스텟 조회
 *   val str      = api.getStrength(player)      // 힘 스텟값
 *   val vit      = api.getVitality(player)      // 체력 스텟값
 *   val agi      = api.getAgility(player)       // 민첩 스텟값
 *   val bonusDmg = api.getBonusDamage(player)   // 힘에 의한 추가 데미지
 *   val dodgeRate= api.getDodgeRate(player)     // 민첩 회피율 (0.0 ~ 0.75)
 *   val bonusHp  = api.getBonusMaxHp(player)    // 체력에 의한 추가 HP
 *
 *   // 스텟 조작 (내부 플러그인용)
 *   api.giveStatPoints(player, 5)
 *   api.setStrength(player, 10)
 *   api.setVitality(player, 10)
 *   api.setAgility(player, 10)
 * ────────────────────────────────────────────────────────────────────────
 */
object CRRPGCoreAPI {

    private var plugin: CRRPGCorePlugin? = null

    internal fun init(instance: CRRPGCorePlugin) { plugin = instance }
    internal fun shutdown() { plugin = null }

    fun getInstance(): CRRPGCoreAPI? = if (plugin != null) this else null

    // ═══════════════════════════════════════════════════════════════════
    //  레벨 / 경험치
    // ═══════════════════════════════════════════════════════════════════

    fun getLevel(player: Player): Int =
        plugin!!.levelManager.getPlayerData(player).level

    fun getLevel(uuid: UUID): Int? =
        Bukkit.getPlayer(uuid)?.let { getLevel(it) }

    fun getCurrentXp(player: Player): Long =
        plugin!!.levelManager.getPlayerData(player).xp

    fun getRequiredXp(player: Player): Long =
        plugin!!.levelManager.getRequiredXpForLevel(getLevel(player))

    fun getRemainingXp(player: Player): Long =
        (getRequiredXp(player) - getCurrentXp(player)).coerceAtLeast(0)

    fun getMaxLevel(): Int =
        plugin!!.levelManager.maxLevel

    fun isMaxLevel(player: Player): Boolean =
        getLevel(player) >= getMaxLevel()

    fun getRequiredXpForLevel(level: Int): Long =
        plugin!!.levelManager.getRequiredXpForLevel(level)

    fun giveXp(player: Player, amount: Int) =
        plugin!!.levelManager.giveXp(player, amount)

    fun setLevel(player: Player, level: Int) =
        plugin!!.levelManager.setLevel(player, level)

    fun setXp(player: Player, xp: Long) =
        plugin!!.levelManager.setXp(player, xp)

    // ═══════════════════════════════════════════════════════════════════
    //  스텟 조회
    // ═══════════════════════════════════════════════════════════════════

    /** 미분배 스텟 포인트 */
    fun getStatPoints(player: Player): Int =
        plugin!!.levelManager.getPlayerData(player).statPoints

    /** 힘 스텟 값 */
    fun getStrength(player: Player): Int =
        plugin!!.levelManager.getPlayerData(player).strength

    /** 체력 스텟 값 */
    fun getVitality(player: Player): Int =
        plugin!!.levelManager.getPlayerData(player).vitality

    /** 민첩 스텟 값 */
    fun getAgility(player: Player): Int =
        plugin!!.levelManager.getPlayerData(player).agility

    /**
     * 힘 스텟에 의한 추가 데미지 (double)
     * 다른 플러그인 데미지 계산 시 이 값을 최종 데미지에 더하세요.
     */
    fun getBonusDamage(player: Player): Double =
        plugin!!.statManager.getBonusDamage(player)

    /**
     * 체력 스텟에 의한 추가 최대 HP
     * ex) vitality=5 → +10 HP
     */
    fun getBonusMaxHp(player: Player): Double {
        val vit = getVitality(player)
        return vit * StatType.VITALITY.effectPerPoint
    }

    /**
     * 민첩 스텟에 의한 회피율 (0.0 ~ 0.75)
     * ex) agility=20 → 0.10 (10%)
     */
    fun getDodgeRate(player: Player): Double {
        val agi = getAgility(player)
        return (agi * StatType.AGILITY.effectPerPoint / 100.0).coerceIn(0.0, 0.75)
    }

    // ═══════════════════════════════════════════════════════════════════
    //  스텟 조작
    // ═══════════════════════════════════════════════════════════════════

    /** 스텟 포인트 직접 지급 */
    fun giveStatPoints(player: Player, amount: Int) =
        plugin!!.statManager.grantPoints(player, amount)

    /** 힘 스텟 강제 설정 */
    fun setStrength(player: Player, value: Int) {
        plugin!!.levelManager.getPlayerData(player).strength = value.coerceAtLeast(0)
    }

    /** 체력 스텟 강제 설정 (HP 속성 즉시 반영) */
    fun setVitality(player: Player, value: Int) {
        val data = plugin!!.levelManager.getPlayerData(player)
        data.vitality = value.coerceAtLeast(0)
        plugin!!.statManager.applyVitality(player, data)
    }

    /** 민첩 스텟 강제 설정 */
    fun setAgility(player: Player, value: Int) {
        plugin!!.levelManager.getPlayerData(player).agility = value.coerceAtLeast(0)
    }
}