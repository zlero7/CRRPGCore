package io.zlero.cRRPGCore

import io.zlero.cRFramework.view.View
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * 스텟 분배 GUI — CRFramework View 기반
 *
 * 레이아웃 (3행 27칸)
 *   Row 0 : [bg][bg][bg][bg][잔여P][bg][bg][bg][bg]
 *   Row 1 : [bg][bg][힘+1][bg][체+1][bg][민+1][bg][bg]
 *   Row 2 : [bg][bg][힘+10][bg][체+10][bg][민+10][bg][bg]
 *
 *   슬롯  4  = 잔여 포인트 표시
 *   슬롯 11  = 힘 +1   /  20 = 힘 +10
 *   슬롯 13  = 체력 +1 /  22 = 체력 +10
 *   슬롯 15  = 민첩 +1 /  24 = 민첩 +10
 *
 * +10 버튼: 포인트·최대치를 감안해 실제로 분배 가능한 만큼(최대 10) 자동 조정
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

        // ── 잔여 포인트 (slot 4, 상단 중앙) ────────────────────────────
        button(slot = 4) {
            item { player ->
                val data = rpg.levelManager.getPlayerData(player)
                val sm   = rpg.statManager
                makeItem(
                    if (data.statPoints > 0) Material.NETHER_STAR else Material.COAL,
                    "§e§l잔여 스텟 포인트 §f[ §f${data.statPoints}P ]",
                    listOf(
                        "§r",
                        "  §e✦ §7레벨업 시 §a${sm.pointsPerLevel}P §7지급",
                        "§r",
                        "  §7좌클릭 §8: §f+1 분배",
                        "  §7하단 버튼 클릭 §8: §f최대 +10 분배"
                    )
                )
            }
        }

        // ── 힘 +1 버튼 (slot 11) ─────────────────────────────────────
        button(slot = 11) {
            item { player ->
                val data      = rpg.levelManager.getPlayerData(player)
                val sm        = rpg.statManager
                val strFull   = data.strength >= sm.maxStrength
                val hasPoints = data.statPoints > 0
                val bonus     = (data.strength * StatType.STRENGTH.effectPerPoint).toInt()
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
                            hasPoints -> add("  §e▶ §f클릭하여 힘 §c+1")
                            else      -> add("  §c✖ §f스텟 포인트가 없습니다")
                        }
                    }
                )
            }
            onClick { player ->
                val data = rpg.levelManager.getPlayerData(player)
                if (data.statPoints <= 0 || data.strength >= rpg.statManager.maxStrength) {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f); return@onClick
                }
                rpg.statManager.allocate(player, StatType.STRENGTH, 1)
                rerender()
            }
        }

        // ── 힘 +10 버튼 (slot 20) ─────────────────────────────────────
        button(slot = 20) {
            item { player ->
                val data  = rpg.levelManager.getPlayerData(player)
                val sm    = rpg.statManager
                val avail = calcAvail(data.statPoints, data.strength, sm.maxStrength)
                makeItemPlus10(
                    Material.REDSTONE,
                    "§c힘", avail,
                    "§cDMG §f+ §c${StatType.STRENGTH.effectPerPoint.toInt()} §7/ 1스텟"
                )
            }
            onClick { player ->
                val data  = rpg.levelManager.getPlayerData(player)
                val sm    = rpg.statManager
                val avail = calcAvail(data.statPoints, data.strength, sm.maxStrength)
                if (avail <= 0) {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f); return@onClick
                }
                rpg.statManager.allocate(player, StatType.STRENGTH, avail)
                rerender()
            }
        }

        // ── 체력 +1 버튼 (slot 13) ───────────────────────────────────
        button(slot = 13) {
            item { player ->
                val data      = rpg.levelManager.getPlayerData(player)
                val sm        = rpg.statManager
                val vitFull   = data.vitality >= sm.maxVitality
                val hasPoints = data.statPoints > 0
                val bonus     = (data.vitality * StatType.VITALITY.effectPerPoint).toInt()
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
                            hasPoints -> add("  §e▶ §f클릭하여 체력 §a+1")
                            else      -> add("  §c✖ §f스텟 포인트가 없습니다")
                        }
                    }
                )
            }
            onClick { player ->
                val data = rpg.levelManager.getPlayerData(player)
                if (data.statPoints <= 0 || data.vitality >= rpg.statManager.maxVitality) {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f); return@onClick
                }
                rpg.statManager.allocate(player, StatType.VITALITY, 1)
                rerender()
            }
        }

        // ── 체력 +10 버튼 (slot 22) ───────────────────────────────────
        button(slot = 22) {
            item { player ->
                val data  = rpg.levelManager.getPlayerData(player)
                val sm    = rpg.statManager
                val avail = calcAvail(data.statPoints, data.vitality, sm.maxVitality)
                makeItemPlus10(
                    Material.APPLE,
                    "§a체력", avail,
                    "§aHP §f+ §a${StatType.VITALITY.effectPerPoint.toInt()} §7/ 1스텟"
                )
            }
            onClick { player ->
                val data  = rpg.levelManager.getPlayerData(player)
                val sm    = rpg.statManager
                val avail = calcAvail(data.statPoints, data.vitality, sm.maxVitality)
                if (avail <= 0) {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f); return@onClick
                }
                rpg.statManager.allocate(player, StatType.VITALITY, avail)
                rerender()
            }
        }

        // ── 민첩 +1 버튼 (slot 15) ───────────────────────────────────
        button(slot = 15) {
            item { player ->
                val data      = rpg.levelManager.getPlayerData(player)
                val sm        = rpg.statManager
                val agiFull   = data.agility >= sm.maxAgility
                val hasPoints = data.statPoints > 0
                val dodgePct  = String.format("%.1f", data.agility * StatType.AGILITY.effectPerPoint)
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
                            hasPoints -> add("  §e▶ §f클릭하여 민첩 §b+1")
                            else      -> add("  §c✖ §f스텟 포인트가 없습니다")
                        }
                    }
                )
            }
            onClick { player ->
                val data = rpg.levelManager.getPlayerData(player)
                if (data.statPoints <= 0 || data.agility >= rpg.statManager.maxAgility) {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f); return@onClick
                }
                rpg.statManager.allocate(player, StatType.AGILITY, 1)
                rerender()
            }
        }

        // ── 민첩 +10 버튼 (slot 24) ───────────────────────────────────
        button(slot = 24) {
            item { player ->
                val data  = rpg.levelManager.getPlayerData(player)
                val sm    = rpg.statManager
                val avail = calcAvail(data.statPoints, data.agility, sm.maxAgility)
                makeItemPlus10(
                    Material.FEATHER,
                    "§b민첩", avail,
                    "§b회피율 §f+ §b${StatType.AGILITY.effectPerPoint}% §7/ 1스텟"
                )
            }
            onClick { player ->
                val data  = rpg.levelManager.getPlayerData(player)
                val sm    = rpg.statManager
                val avail = calcAvail(data.statPoints, data.agility, sm.maxAgility)
                if (avail <= 0) {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f); return@onClick
                }
                rpg.statManager.allocate(player, StatType.AGILITY, avail)
                rerender()
            }
        }
    }

    override fun onClose(player: Player) {
        rpg.playerDataRepository.flush(player.uniqueId)
    }

    // ── 실제 분배 가능 수량 (최대 10, 포인트·최대치 동시 고려) ──────────
    private fun calcAvail(points: Int, current: Int, max: Int): Int =
        points.coerceAtMost(10).coerceAtMost(max - current).coerceAtLeast(0)

    // ── +1 버튼용 아이템 생성 ─────────────────────────────────────────
    private fun makeItem(mat: Material, name: String, lore: List<String> = emptyList()): ItemStack =
        ItemStack(mat).apply {
            itemMeta = itemMeta?.also {
                it.setDisplayName(name)
                it.lore = lore
            }
        }

    // ── +10 버튼용 아이템 생성 ────────────────────────────────────────
    private fun makeItemPlus10(
        mat: Material,
        statName: String,
        avail: Int,
        effectDesc: String
    ): ItemStack {
        val canAllocate = avail > 0
        return ItemStack(mat).apply {
            itemMeta = itemMeta?.also {
                it.setDisplayName(
                    if (canAllocate) "§e▶ §f$statName §e+$avail §7분배"
                    else             "§8✖ §7$statName §8+10 §7(포인트 또는 최대치 부족)"
                )
                it.lore = buildList {
                    add("§r")
                    add("  §7효과 §8: $effectDesc")
                    add("§r")
                    if (canAllocate)
                        add("  §e▶ §f클릭하여 §e${avail}P §f한 번에 분배")
                    else
                        add("  §c✖ §f분배 가능한 포인트가 없습니다")
                }
            }
        }
    }
}
