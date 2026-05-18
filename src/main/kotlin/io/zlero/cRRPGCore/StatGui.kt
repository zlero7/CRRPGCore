package io.zlero.cRRPGCore

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

object StatGui : Listener {

    private const val TITLE       = "§8⚔ §b스텟 분배 §8⚔"
    private const val SLOT_STR    = 11
    private const val SLOT_VIT    = 13
    private const val SLOT_AGI    = 15
    private const val SLOT_POINTS = 22

    fun open(player: Player, plugin: CRRPGCorePlugin) {
        val inv = Bukkit.createInventory(null, 27, TITLE)
        refresh(inv, player, plugin)
        player.openInventory(inv)
    }

    private fun refresh(inv: Inventory, player: Player, plugin: CRRPGCorePlugin) {
        val data      = plugin.levelManager.getPlayerData(player)
        val statMgr   = plugin.statManager
        val hasPoints = data.statPoints > 0

        val filler = item(Material.GRAY_STAINED_GLASS_PANE, "§r")
        for (i in 0 until 27) inv.setItem(i, filler)

        val strFull  = data.strength >= statMgr.maxStrength
        val strBonus = (data.strength * StatType.STRENGTH.effectPerPoint).toInt()
        inv.setItem(SLOT_STR, item(
            Material.REDSTONE,
            "§c§l힘  §8[ §f${data.strength} §8/ §7${statMgr.maxStrength} §8]",
            buildList {
                add("§r")
                add("  §8✦ §71 스텟당 §cDMG §f+ §c${StatType.STRENGTH.effectPerPoint.toInt()}")
                add("  §8✦ §7현재 보너스 §8: §cDMG §f+ §c$strBonus")
                add("§r")
                when {
                    strFull   -> add("  §6▶ §e최대치에 도달했습니다")
                    hasPoints -> add("  §e▶ §f클릭하여 힘 +1")
                    else      -> add("  §c✖ §f스텟 포인트가 없습니다")
                }
            }
        ))

        val vitFull  = data.vitality >= statMgr.maxVitality
        val vitBonus = (data.vitality * StatType.VITALITY.effectPerPoint).toInt()
        inv.setItem(SLOT_VIT, item(
            Material.APPLE,
            "§a§l체력  §8[ §f${data.vitality} §8/ §7${statMgr.maxVitality} §8]",
            buildList {
                add("§r")
                add("  §8✦ §71 스텟당 §aHP §f+ §a${StatType.VITALITY.effectPerPoint.toInt()}")
                add("  §8✦ §7현재 보너스 §8: §aHP §f+ §a$vitBonus")
                add("§r")
                when {
                    vitFull   -> add("  §6▶ §e최대치에 도달했습니다")
                    hasPoints -> add("  §e▶ §f클릭하여 체력 +1")
                    else      -> add("  §c✖ §f스텟 포인트가 없습니다")
                }
            }
        ))

        val agiFull  = data.agility >= statMgr.maxAgility
        val dodgePct = String.format("%.1f", data.agility * StatType.AGILITY.effectPerPoint)
        inv.setItem(SLOT_AGI, item(
            Material.FEATHER,
            "§b§l민첩  §8[ §f${data.agility} §8/ §7${statMgr.maxAgility} §8]",
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
        ))

        inv.setItem(SLOT_POINTS, item(
            if (hasPoints) Material.NETHER_STAR else Material.COAL,
            "§e§l잔여 스텟 포인트 §f[ §f${data.statPoints}P ]",
            listOf(
                "§r",
                "  §e✦ §7레벨업 시 §a${statMgr.pointsPerLevel}P §7지급"
            )
        ))
    }

    // ─── 스텟 클릭 ───────────────────────────────────────────────────────
    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (event.view.title() != net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize(TITLE)) return

        event.isCancelled = true

        val player  = event.whoClicked as? Player ?: return
        val plugin  = CRRPGCorePlugin.plugin
        val statMgr = plugin.statManager
        val data    = plugin.levelManager.getPlayerData(player)

        val stat = when (event.slot) {
            SLOT_STR -> StatType.STRENGTH
            SLOT_VIT -> StatType.VITALITY
            SLOT_AGI -> StatType.AGILITY
            else     -> return
        }

        val current = when (stat) {
            StatType.STRENGTH -> data.strength
            StatType.VITALITY -> data.vitality
            StatType.AGILITY  -> data.agility
        }
        val max = when (stat) {
            StatType.STRENGTH -> statMgr.maxStrength
            StatType.VITALITY -> statMgr.maxVitality
            StatType.AGILITY  -> statMgr.maxAgility
        }

        if (data.statPoints <= 0 || current >= max) {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        // ★ 스텟 찍을 때마다 즉시 저장
        statMgr.allocate(player, stat)
        plugin.playerDataManager.savePlayerAsync(player.uniqueId, data)
        refresh(event.inventory, player, plugin)
    }

    // ─── GUI 닫을 때 저장 ────────────────────────────────────────────────
    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        if (event.view.title() != net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize(TITLE)) return

        val player = event.player as? Player ?: return
        val plugin = CRRPGCorePlugin.plugin
        val data   = plugin.levelManager.getPlayerData(player)

        // ★ GUI 닫을 때 저장
        plugin.playerDataManager.savePlayerAsync(player.uniqueId, data)
    }

    private fun item(mat: Material, name: String, lore: List<String> = emptyList()): ItemStack {
        val stack = ItemStack(mat)
        val meta: ItemMeta = stack.itemMeta
        meta.setDisplayName(name)
        meta.lore = lore
        stack.itemMeta = meta
        return stack
    }
}