package io.zlero.cRRPGCore.listener

import io.zlero.cRRPGCore.CRRPGCorePlugin
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer

class RpgCorePlaceholderExpansion(private val plugin: CRRPGCorePlugin) : PlaceholderExpansion() {

    override fun getIdentifier() = "crrpg"
    override fun getAuthor() = "zlero"
    override fun getVersion() = plugin.description.version
    override fun persist() = true
    override fun canRegister() = true

    /**
     * 사용 가능한 플레이스홀더:
     *   %crrpg_level%            — 현재 레벨
     *   %crrpg_xp%               — 현재 경험치
     *   %crrpg_xp_required%      — 다음 레벨까지 필요 경험치
     *   %crrpg_stat_strength%    — 힘 스탯
     *   %crrpg_stat_vitality%    — 체력 스탯
     *   %crrpg_stat_agility%     — 민첩 스탯
     *   %crrpg_stat_points%      — 잔여 스탯 포인트
     *   %crrpg_rank_level%       — 레벨 기준 서버 순위 (온라인 플레이어 중)
     *   %crrpg_rank_level_top_N% — 레벨 순위 N위 플레이어 이름 (N=1~10)
     *   %crrpg_rank_level_top_N_value% — N위 플레이어 레벨
     */
    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        val repo = plugin.playerDataRepository
        val lm   = plugin.levelManager

        return when {
            params == "level" -> {
                val uuid = player?.uniqueId ?: return "0"
                (repo.get(uuid)?.level ?: 0).toString()
            }
            params == "xp" -> {
                val uuid = player?.uniqueId ?: return "0"
                (repo.get(uuid)?.xp ?: 0L).toString()
            }
            params == "xp_required" -> {
                val uuid  = player?.uniqueId ?: return "0"
                val level = repo.get(uuid)?.level ?: 1
                lm.getRequiredXpForLevel(level).toString()
            }
            params == "stat_strength" -> {
                val uuid = player?.uniqueId ?: return "0"
                (repo.get(uuid)?.strength ?: 0).toString()
            }
            params == "stat_vitality" -> {
                val uuid = player?.uniqueId ?: return "0"
                (repo.get(uuid)?.vitality ?: 0).toString()
            }
            params == "stat_agility" -> {
                val uuid = player?.uniqueId ?: return "0"
                (repo.get(uuid)?.agility ?: 0).toString()
            }
            params == "stat_points" -> {
                val uuid = player?.uniqueId ?: return "0"
                (repo.get(uuid)?.statPoints ?: 0).toString()
            }
            params == "rank_level" -> {
                val uuid = player?.uniqueId ?: return "-"
                val myLevel = repo.get(uuid)?.level ?: 0
                val rank = org.bukkit.Bukkit.getOnlinePlayers()
                    .count { (repo.get(it.uniqueId)?.level ?: 0) > myLevel } + 1
                rank.toString()
            }
            params.startsWith("rank_level_top_") -> {
                val rest = params.removePrefix("rank_level_top_")
                val parts = rest.split("_")
                val n = parts[0].toIntOrNull()?.coerceIn(1, 10) ?: return null
                val wantValue = parts.getOrNull(1) == "value"
                val sorted = org.bukkit.Bukkit.getOnlinePlayers()
                    .sortedByDescending { repo.get(it.uniqueId)?.level ?: 0 }
                val target = sorted.getOrNull(n - 1) ?: return if (wantValue) "0" else "-"
                if (wantValue) (repo.get(target.uniqueId)?.level ?: 0).toString()
                else target.name
            }
            else -> null
        }
    }
}
