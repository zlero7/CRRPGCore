package io.zlero.cRRPGCore.view

import io.zlero.cRRPGCore.CRRPGCorePlugin
import io.zlero.cRRPGCore.model.PlayerData
import io.zlero.cRRPGCore.model.StatType
import io.zlero.cRFramework.view.View
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * 스텟 분배 GUI — CRFramework View 기반
 *
 * 레이아웃 (1행 9칸)
 *   [bg][bg][힘][bg][체력][bg][민첩][bg][bg]
 *
 *   좌클릭 : +1 분배
 *   우클릭 : 최대 +10 분배 (포인트·최대치 감안하여 자동 조정)
 */
class StatView(private val rpg: CRRPGCorePlugin)
    : View(rpg, "§8⚔ §b스텟 분배 §8⚔", rows = 1) {

    override fun io.zlero.cRFramework.view.scope.CreateScope.onCreate() {

        // ── 배경 채우기 ──────────────────────────────────────────────────
        fill(rows = 1) {
            item(ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
                itemMeta = itemMeta?.also { it.setDisplayName("§r") }
            })
        }

        // ── 힘 (slot 2) ─────────────────────────────────────────────────
        button(slot = 2) {
            item { player -> makeStatItem(player, StatType.STRENGTH) }
            onClick { player ->
                if (tryAllocate(player, StatType.STRENGTH, 1)) rerender()
            }
            onRightClick { player ->
                val amt = calcAvail(player, StatType.STRENGTH)
                if (tryAllocate(player, StatType.STRENGTH, amt)) rerender()
            }
        }

        // ── 체력 (slot 4) ────────────────────────────────────────────────
        button(slot = 4) {
            item { player -> makeStatItem(player, StatType.VITALITY) }
            onClick { player ->
                if (tryAllocate(player, StatType.VITALITY, 1)) rerender()
            }
            onRightClick { player ->
                val amt = calcAvail(player, StatType.VITALITY)
                if (tryAllocate(player, StatType.VITALITY, amt)) rerender()
            }
        }

        // ── 민첩 (slot 6) ────────────────────────────────────────────────
        button(slot = 6) {
            item { player -> makeStatItem(player, StatType.AGILITY) }
            onClick { player ->
                if (tryAllocate(player, StatType.AGILITY, 1)) rerender()
            }
            onRightClick { player ->
                val amt = calcAvail(player, StatType.AGILITY)
                if (tryAllocate(player, StatType.AGILITY, amt)) rerender()
            }
        }
    }

    override fun onClose(player: Player) {
        rpg.playerDataRepository.flush(player.uniqueId)
    }

    // ── 스텟 버튼 아이템 생성 ─────────────────────────────────────────────
    private fun makeStatItem(player: Player, type: StatType): ItemStack {
        val data    = rpg.levelManager.getPlayerData(player)
        val sm      = rpg.statManager
        val current = sm.getStatCurrent(data, type)
        val max     = sm.getStatMax(type)
        val isFull  = current >= max
        val avail   = calcAvail(data, type)

        val (mat, color, effectLine, bonusLine) = when (type) {
            StatType.STRENGTH -> StatDisplay(
                Material.REDSTONE, "§c",
                "§71스텟당 §8: §c+${StatType.STRENGTH.effectPerPoint.toInt()} §7데미지",
                "§7현재 보너스 §8: §cDMG §f+ §c${(current * StatType.STRENGTH.effectPerPoint).toInt()}"
            )
            StatType.VITALITY -> StatDisplay(
                Material.APPLE, "§a",
                "§71스텟당 §8: §a+${StatType.VITALITY.effectPerPoint.toInt()} §7최대 HP",
                "§7현재 보너스 §8: §aHP §f+ §a${(current * StatType.VITALITY.effectPerPoint).toInt()}"
            )
            StatType.AGILITY  -> StatDisplay(
                Material.FEATHER, "§b",
                "§71스텟당 §8: §b+${StatType.AGILITY.effectPerPoint}% §7회피율",
                "§7현재 보너스 §8: §b회피율 §f+ §b${String.format("%.1f", current * StatType.AGILITY.effectPerPoint)}%"
            )
        }

        val name = "${color}${type.displayName}  §8[ §f$current §8/ §7$max §8]"
        val lore = buildList {
            add("§r")
            add("  $effectLine")
            add("  $bonusLine")
            add("§r")
            if (isFull) {
                add("  §e▶ §e스텟이 최대치에 도달했습니다")
            } else {
                add("  §7좌클릭 §8: §f+1")
                add("  §7우클릭 §8: §f+10 §8(최대 §e${avail}P§8)")
            }
            add("§r")
            if (isFull || data.statPoints > 0) {
                add("  §7잔여 포인트 §8: §e${data.statPoints}P")
            } else {
                add("  §c✖ §f스텟 포인트가 없습니다")
            }
        }

        return ItemStack(mat).apply {
            itemMeta = itemMeta?.also {
                it.setDisplayName(name)
                it.lore = lore
            }
        }
    }

    // ── 분배 시도 (성공 시 true 반환) ──────────────────────────────────────
    private fun tryAllocate(player: Player, type: StatType, amount: Int): Boolean {
        if (amount <= 0) {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return false
        }
        val ok = rpg.statManager.allocate(player, type, amount)
        if (ok) {
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f)
        } else {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
        }
        return ok
    }

    // ── 실제 분배 가능 수량 (최대 10, 포인트·최대치 동시 고려) ──────────────
    private fun calcAvail(player: Player, type: StatType): Int =
        calcAvail(rpg.levelManager.getPlayerData(player), type)

    private fun calcAvail(data: PlayerData, type: StatType): Int {
        val sm   = rpg.statManager
        val left = sm.getStatMax(type) - sm.getStatCurrent(data, type)
        return data.statPoints.coerceAtMost(10).coerceAtMost(left).coerceAtLeast(0)
    }

    private data class StatDisplay(
        val mat       : Material,
        val color     : String,
        val effectLine: String,
        val bonusLine : String
    )
}
