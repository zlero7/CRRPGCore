package io.zlero.cRRPGCore.manager

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class XpBoostManager {

    data class BoostInfo(
        val multiplier: Double,
        val endTime: Long  // System.currentTimeMillis() 기준
    )

    // 개인 부스트
    private val personalBoosts = ConcurrentHashMap<UUID, BoostInfo>()
    // 전체 부스트 (한 명만 활성화 가능)
    private var globalBoost: BoostInfo? = null

    /** false 반환 시 이미 활성화 중 */
    fun setPersonalBoost(uuid: UUID, multiplier: Double, minutes: Int): Boolean {
        val current = personalBoosts[uuid]
        if (current != null && current.endTime > System.currentTimeMillis()) return false
        personalBoosts[uuid] = BoostInfo(
            multiplier = multiplier,
            endTime    = System.currentTimeMillis() + minutes * 60_000L
        )
        return true
    }

    fun setGlobalBoost(multiplier: Double, minutes: Int): Boolean {
        val current = globalBoost
        if (current != null && current.endTime > System.currentTimeMillis()) {
            return false  // 이미 전체 부스트 활성화 중
        }
        globalBoost = BoostInfo(
            multiplier = multiplier,
            endTime    = System.currentTimeMillis() + minutes * 60_000L
        )
        return true
    }

    /** 최종 배수 = 개인 배수 + 전체 배수 - 1.0 (중첩 시 합산, 각각 만료 시 1.0) */
    fun getTotalMultiplier(uuid: UUID): Double {
        val now = System.currentTimeMillis()
        val personal = personalBoosts[uuid]?.let {
            if (it.endTime > now) it.multiplier else { personalBoosts.remove(uuid); 0.0 }
        } ?: 0.0
        val global = globalBoost?.let {
            if (it.endTime > now) it.multiplier else { globalBoost = null; 0.0 }
        } ?: 0.0
        return (personal + global).coerceAtLeast(1.0)
    }

    /** 만료된 개인 부스트 항목을 일괄 제거 (주기적 태스크에서 호출) */
    fun pruneExpired() {
        val now = System.currentTimeMillis()
        personalBoosts.entries.removeIf { it.value.endTime <= now }
        if (globalBoost?.endTime?.let { it <= now } == true) globalBoost = null
    }

    fun getPersonalBoost(uuid: UUID): BoostInfo? {
        val boost = personalBoosts[uuid] ?: return null
        return if (boost.endTime > System.currentTimeMillis()) boost else {
            personalBoosts.remove(uuid)
            null
        }
    }

    fun getGlobalBoost(): BoostInfo? {
        val boost = globalBoost ?: return null
        return if (boost.endTime > System.currentTimeMillis()) boost else {
            globalBoost = null
            null
        }
    }

    fun hasGlobalBoost(): Boolean = getGlobalBoost() != null

    /** 액션바 표시용 문자열. 부스트 없으면 null */
    fun getActionBarSegment(uuid: UUID): String? {
        val now      = System.currentTimeMillis()
        val personal = personalBoosts[uuid]?.takeIf { it.endTime > now }
        val global   = globalBoost?.takeIf { it.endTime > now }

        val lines = mutableListOf<String>()
        if (global   != null) lines.add("§6${String.format("%.1f", global.multiplier)}x §7(${remainStr(global.endTime - now)})")
        if (personal != null) lines.add("§e${String.format("%.1f", personal.multiplier)}x §7(${remainStr(personal.endTime - now)})")

        return if (lines.isEmpty()) null
        else "§8[ ${lines.joinToString(" §8+ ")} §8]"
    }

    private fun remainStr(msLeft: Long): String {
        val totalSec = (msLeft / 1000).coerceAtLeast(0)
        val min      = totalSec / 60
        val sec      = totalSec % 60
        return if (min > 0) "${min}분 ${sec}초" else "${sec}초"
    }
}
