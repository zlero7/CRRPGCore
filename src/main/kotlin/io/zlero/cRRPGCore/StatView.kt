package io.zlero.cRRPGCore

import io.zlero.cRFramework.view.View
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * 스텟 분배 GUI — CRFramework View 기반
 *
 * - CRPlugin.onEnable()에서 ViewListener가 자동 등록되어 클릭 라우팅
 * - pm.registerEvents() 불필요 (object Listener 방식 대체)
 * - 슬롯 클릭 → allocate() → rerender() 로 반응형 갱신
 * - onClose() → flush (dirty 데이터 즉시 DB 저장)
 */
class StatView(private val rpg: CRRPGCorePlugin)
    : View(rpg, "§8⚔ §b스텟 분배 §8⚔", rows = 3) {

    override fun io.zlero.cRFramework.view.scope.CreateScope.onCreate() {

        // ── 배경 채우기 ──────────────────────────────────────────────────
        fill(rows = 3) {
            item(ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
                itemMeta = itemMeta?.also { it.setDisplayName("§r") }
            })
        }

        // ── 힘 버튼 (slot 11) ────────────────────────────────────────────
        button(slot = 11) {
            item { player ->
                val data     = rpg.levelManager.getPlayerData(player)
                val sm       = rpg.statManager
                val strFull  = data.strength >= sm.maxStrength
                val hasPoints = data.statPoints > 0
                val bonus    = (data.strength * StatType.STRENGTH.effectPerPoint).toInt()
                makeItem(
                    Material.REDSTONE,
                    "§c§l힘  §8[ §f${data.strength} §8/ §7${sm.maxStrength} §8]",
                    buildList {
                        add("§r")
                        add("  §8✦ §71 스텟당 §cDMG §f+ §c${StatType.STRENGTH.effectPerPoint.toInt()}")
                        add("  §8✦ §7현재 보너스 §8: §cDMG §f+ §c$bonus")
                        add("§r")
                        when {
                            strFull   -> add("  §6▶ §e최대치에 도달했습니다")
                            hasPoints -> add("  §e▶ §f클릭하여 힘 +1")
                            else      -> add("  §c✖ §f스텟 포인트가 없습니다")
                        }
                    }
                )
            }
            onClick { player ->
                val data = rpg.levelManager.getPlayerData(player)
                if (data.statPoints <= 0 || data.strength >= rpg.statManager.maxStrength) {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                    return@onClick
                }
                rpg.statManager.allocate(player, StatType.STRENGTH)
                rerender()
            }
        }

        // ── 체력 버튼 (slot 13) ──────────────────────────────────────────
        button(slot = 13) {
            item { player ->
                val data     = rpg.levelManager.getPlayerData(player)
                val sm       = rpg.statManager
                val vitFull  = data.vitality >= sm.maxVitality
                val hasPoints = data.statPoints > 0
                val bonus    = (data.vitality * StatType.VITALITY.effectPerPoint).toInt()
                makeItem(
                    Material.APPLE,
                    "§a§l체력  §8[ §f${data.vitality} §8/ §7${sm.maxVitality} §8]",
                    buildList {
                        add("§r")
                        add("  §8✦ §71 스텟당 §aHP §f+ §a${StatType.VITALITY.effectPerPoint.toInt()}")
                        add("  §8✦ §7현재 보너스 §8: §aHP §f+ §a$bonus")
                        add("§r")
                        when {
                            vitFull   -> add("  §6▶ §e최대치에 도달했습니다")
                            hasPoints -> add("  §e▶ §f클릭하여 체력 +1")
                            else      -> add("  §c✖ §f스텟 포인트가 없습니다")
                        }
                    }
                )
            }
            onClick { player ->
                val data = rpg.levelManager.getPlayerData(player)
                if (data.statPoints <= 0 || data.vitality >= rpg.statManager.maxVitality) {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                    return@onClick
                }
                rpg.statManager.allocate(player, StatType.VITALITY)
                rerender()
            }
        }

        // ── 민첩 버튼 (slot 15) ──────────────────────────────────────────
        button(slot = 15) {
            item { player ->
                val data     = rpg.levelManager.getPlayerData(player)
                val sm       = rpg.statManager
                val agiFull  = data.agility >= sm.maxAgility
                val hasPoints = data.statPoints > 0
                val dodgePct = String.format("%.1f", data.agility * StatType.AGILITY.effectPerPoint)
                makeItem(
                    Material.FEATHER,
                    "§b§l민첩  §8[ §f${data.agility} §8/ §7${sm.maxAgility} §8]",
                    buildList {
                        add("§r")
                        add("  §8✦ §71 스텟당 §b회피율 §f+ §b${StatType.AGILITY.effectPerPoint}%")
                        add("  §8✦ §7현재 보너스 §8: §b회피율 §f+ §b$dodgePct%")
                        add("§r")
                        when {
                            agiFull   -> add("  §6▶ §e최대치에 도달했습니다")
                            hasPoints -> add("  §e▶ §f클릭하여 민첩 +1")
                            else      -> add("  §c✖ §f스텟 포인트가 없습니다")
                        }
                    }
                )
            }
            onClick { player ->
                val data = rpg.levelManager.getPlayerData(player)
                if (data.statPoints <= 0 || data.agility >= rpg.statManager.maxAgility) {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                    return@onClick
                }
                rpg.statManager.allocate(player, StatType.AGILITY)
                rerender()
            }
        }

        // ── 잔여 포인트 표시 (slot 22, 클릭 없음) ──────────────────────
        button(slot = 22) {
            item { player ->
                val data = rpg.levelManager.getPlayerData(player)
                val sm   = rpg.statManager
                makeItem(
                    if (data.statPoints > 0) Material.NETHER_STAR else Material.COAL,
                    "§e§l잔여 스텟 포인트 §f[ §f${data.statPoints}P ]",
                    listOf(
                        "§r",
                        "  §e✦ §7레벨업 시 §a${sm.pointsPerLevel}P §7지급"
                    )
                )
            }
        }
    }

    override fun onClose(player: Player) {
        // dirty 데이터 즉시 flush (퇴장 시 자동 저장도 되지만 GUI 닫을 때 즉시 반영)
        rpg.playerDataRepository.flush(player.uniqueId)
    }

    private fun makeItem(mat: Material, name: String, lore: List<String> = emptyList()): ItemStack =
        ItemStack(mat).apply {
            itemMeta = itemMeta?.also {
                it.setDisplayName(name)
                it.lore = lore
            }
        }
}
