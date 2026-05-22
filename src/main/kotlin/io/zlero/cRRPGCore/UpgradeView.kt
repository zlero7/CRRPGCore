package io.zlero.cRRPGCore

import io.zlero.cRFramework.view.View
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 강화 GUI — CRFramework View 기반
 *
 * 상단 인벤토리(4행 36칸): 슬롯 버튼 클릭 → 아이템 반환 / 강화 버튼 → 강화 실행
 * 하단 인벤토리 클릭    : GUIBottomClickListener → 아이템 종류 감지 후 알맞은 슬롯에 자동 배치
 *
 * 닫힘 시: 미수령 아이템 전부 인벤토리로 반환
 */
class UpgradeView(private val rpg: CRRPGCorePlugin)
    : View(rpg, rpg.msgCfg.guiUpgradeTitle, rows = 4) {

    var itemSlot:     ItemStack? = null
    var stoneSlot:    ItemStack? = null
    var breakProSlot: ItemStack? = null
    var downProSlot:  ItemStack? = null
    var resultItem:   ItemStack? = null

    companion object {
        const val SLOT_ITEM      = 11
        const val SLOT_STONE     = 13
        const val SLOT_BREAK_PRO = 20
        const val SLOT_DOWN_PRO  = 21
        const val SLOT_RESULT    = 15
        const val SLOT_RETRY     = 24   // 결과 슬롯(15) 아래 — 재강화 버튼
        val UPGRADE_BTN_SLOTS    = setOf(25, 26, 34, 35)

        private val openViews = ConcurrentHashMap<UUID, UpgradeView>()

        fun openFor(rpg: CRRPGCorePlugin, player: Player) {
            val view = UpgradeView(rpg)
            openViews[player.uniqueId] = view
            view.open(player)
        }

        fun isOpen(uuid: UUID) = openViews.containsKey(uuid)
        fun getView(uuid: UUID): UpgradeView? = openViews[uuid]

        fun closeAll() {
            val uuids = ArrayList(openViews.keys)
            openViews.clear()
            uuids.forEach { uuid ->
                CRRPGCorePlugin.plugin.server.getPlayer(uuid)?.closeInventory()
            }
        }
    }

    override fun io.zlero.cRFramework.view.scope.CreateScope.onCreate() {
        val mc = rpg.msgCfg

        // 배경 채우기
        fill(rows = 4) {
            item(makeItem(Material.GRAY_STAINED_GLASS_PANE, "§r"))
        }

        // 아이템 슬롯
        button(slot = SLOT_ITEM) {
            item { _ ->
                itemSlot ?: makeItem(Material.LIME_STAINED_GLASS_PANE,
                    mc.guiUpgradeSlotItemName, mc.guiUpgradeSlotItemLore)
            }
            onClick { player ->
                val item = itemSlot ?: return@onClick
                itemSlot = null
                player.inventory.addItem(item)
                rerender()
            }
        }

        // 강화석 슬롯
        button(slot = SLOT_STONE) {
            item { _ ->
                stoneSlot ?: makeItem(Material.YELLOW_STAINED_GLASS_PANE,
                    mc.guiUpgradeSlotStoneName, mc.guiUpgradeSlotStoneLore)
            }
            onClick { player ->
                val item = stoneSlot ?: return@onClick
                stoneSlot = null
                player.inventory.addItem(item)
                rerender()
            }
        }

        // 파괴방지 슬롯
        button(slot = SLOT_BREAK_PRO) {
            item { _ ->
                breakProSlot ?: makeItem(Material.RED_STAINED_GLASS_PANE,
                    mc.guiUpgradeSlotBreakName, mc.guiUpgradeSlotBreakLore)
            }
            onClick { player ->
                val item = breakProSlot ?: return@onClick
                breakProSlot = null
                player.inventory.addItem(item)
                rerender()
            }
        }

        // 하락방지 슬롯
        button(slot = SLOT_DOWN_PRO) {
            item { _ ->
                downProSlot ?: makeItem(Material.ORANGE_STAINED_GLASS_PANE,
                    mc.guiUpgradeSlotDownName, mc.guiUpgradeSlotDownLore)
            }
            onClick { player ->
                val item = downProSlot ?: return@onClick
                downProSlot = null
                player.inventory.addItem(item)
                rerender()
            }
        }

        // 결과 슬롯
        button(slot = SLOT_RESULT) {
            item { _ ->
                resultItem ?: makeItem(Material.WHITE_STAINED_GLASS_PANE,
                    mc.guiUpgradeSlotResultName, mc.guiUpgradeSlotResultLore)
            }
            onClick { player ->
                val res = resultItem ?: return@onClick
                resultItem = null
                player.inventory.addItem(res)
                player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 1f, 1f)
                rerender()
            }
        }

        // 재강화 버튼 (slot 24 — 결과 슬롯 아래)
        // 결과 아이템이 있고 추가 강화 가능할 때만 활성화
        button(slot = SLOT_RETRY) {
            item { _ ->
                val res = resultItem
                when {
                    res == null ->
                        makeItem(Material.GRAY_STAINED_GLASS_PANE, "§8✖ §7재강화 §8(결과 아이템 없음)")
                    rpg.upgradeManager.getLevel(res) >= 10 ->
                        makeItem(Material.NETHER_STAR, "§6★ §e최대 강화 달성!")
                    else ->
                        makeItem(Material.ANVIL, "§a▶ §f재강화",
                            listOf("§r", "  §7결과 아이템을 강화 슬롯으로 이동", "  §e클릭하여 바로 재시도"))
                }
            }
            onClick { player ->
                val res = resultItem ?: return@onClick
                if (rpg.upgradeManager.getLevel(res) >= 10) return@onClick
                // 기존 아이템 슬롯 아이템이 있으면 반환
                itemSlot?.let { player.inventory.addItem(it) }
                itemSlot   = res
                resultItem = null
                rerender()
            }
        }

        // 강화 버튼 (4칸)
        for (slot in UPGRADE_BTN_SLOTS) {
            button(slot = slot) {
                item { player -> buildUpgradeButton(player) }
                onClick { player -> performUpgrade(player) }
            }
        }
    }

    override fun onClose(player: Player) {
        openViews.remove(player.uniqueId)
        listOfNotNull(itemSlot, stoneSlot, breakProSlot, downProSlot, resultItem)
            .forEach { player.inventory.addItem(it) }
    }

    // ─────────────────────────────────────────────────────────────────
    //  강화 버튼 아이템 생성
    // ─────────────────────────────────────────────────────────────────
    private fun buildUpgradeButton(player: Player): ItemStack {
        val mc     = rpg.msgCfg
        val upgMgr = rpg.upgradeManager
        val item   = itemSlot
        val stone  = stoneSlot

        return when {
            item == null ->
                makeItem(Material.GRAY_STAINED_GLASS_PANE, mc.guiUpgradeBtnNoItem, mc.guiUpgradeBtnNoItemLore)

            !rpg.rpgItemManager.isRpgItem(item) ->
                makeItem(Material.BARRIER, mc.guiUpgradeBtnNotRpg, mc.guiUpgradeBtnNotRpgLore)

            upgMgr.getLevel(item) >= 10 ->
                makeItem(Material.NETHER_STAR, mc.guiUpgradeBtnMax, mc.guiUpgradeBtnMaxLore)

            else -> {
                val curLv    = upgMgr.getLevel(item)
                val tgtLv    = curLv + 1
                val chance   = upgMgr.getChance(tgtLv)
                val stoneOk  = stone != null && upgMgr.getStoneType(stone) == upgMgr.requiredStoneType(tgtLv)
                val hasBreak = upgMgr.isProtectBreak(breakProSlot)
                val hasDown  = upgMgr.isProtectDown(downProSlot)

                var dBreak = chance.breakPct
                var dDown  = chance.downPct
                val dSucc  = chance.successPct
                val dKeep  = chance.keepPct
                when {
                    hasBreak && hasDown -> { dBreak = 0.0; dDown = 0.0 }
                    hasBreak            -> dBreak = 0.0
                    hasDown             -> dDown  = 0.0
                }

                val dmgBonus = upgMgr.getDamageBonus(tgtLv)
                val lore = mutableListOf(
                    "§r",
                    "  §7강화 §8: §f+$curLv §8→ §6+$tgtLv",
                    "  §7필요 §8: ${upgMgr.requiredStoneName(tgtLv)}",
                    "  §7무기 데미지 보너스 §8: §c+$dmgBonus",
                    "§r",
                    "  §a성공  §f${fmt(dSucc)}%",
                    "  §7실패  §f${fmt(dKeep)}%",
                    "  §e하락  §f${fmt(dDown)}%${if (hasDown) " §8(→실패 전환)" else ""}",
                    "  §c파괴  §f${fmt(dBreak)}%${if (hasBreak) " §8(→실패 전환)" else ""}",
                    "§r",
                    if (stoneOk) mc.guiUpgradeBtnLoreStoneOk else mc.guiUpgradeBtnLoreStoneErr
                )
                val btnName = if (stoneOk)
                    mc.format(mc.guiUpgradeBtnReady, "cur" to curLv.toString(), "tgt" to tgtLv.toString())
                else mc.guiUpgradeBtnNotReady
                makeItem(if (stoneOk) Material.ANVIL else Material.BARRIER, btnName, lore)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  강화 실행
    // ─────────────────────────────────────────────────────────────────
    private fun performUpgrade(player: Player) {
        val mc     = rpg.msgCfg
        val upgMgr = rpg.upgradeManager
        val item   = itemSlot
        val stone  = stoneSlot

        if (item == null)                          { player.sendMessage(mc.errNeedItem); return }
        if (!rpg.rpgItemManager.isRpgItem(item))   { player.sendMessage(mc.errNotRpgItemUpgrade); return }
        if (stone == null)                         { player.sendMessage(mc.errNeedStone); return }

        val curLv = upgMgr.getLevel(item)
        if (curLv >= 10) { player.sendMessage(mc.errMaxUpgrade); return }

        val tgtLv = curLv + 1
        if (upgMgr.getStoneType(stone) != upgMgr.requiredStoneType(tgtLv)) {
            player.sendMessage(mc.format(mc.errWrongStone, "required" to upgMgr.requiredStoneName(tgtLv)))
            return
        }

        val hasBreak = upgMgr.isProtectBreak(breakProSlot)
        val hasDown  = upgMgr.isProtectDown(downProSlot)

        val result = upgMgr.tryUpgrade(item, hasBreak, hasDown) ?: return

        stoneSlot = null

        when (result.outcome) {
            UpgradeManager.UpgradeOutcome.FAIL_BREAK -> if (hasBreak) breakProSlot = null
            UpgradeManager.UpgradeOutcome.FAIL_DOWN  -> if (hasDown)  downProSlot  = null
            else -> {}
        }

        val itemName = item.itemMeta?.displayName ?: item.type.name

        when (result.outcome) {
            UpgradeManager.UpgradeOutcome.SUCCESS -> {
                resultItem = result.resultItem; itemSlot = null
                // 강화 성공 시 자동 귀속 (config upgrade.auto-bind: true 일 때)
                if (rpg.config.getBoolean("upgrade.auto-bind", false) && result.resultItem != null) {
                    rpg.rpgItemManager.bindItem(result.resultItem!!, player.uniqueId, player.name)
                }
                player.sendMessage(mc.format(mc.msgUpgradeSuccess,
                    "prev" to result.prevLevel.toString(), "new" to result.newLevel.toString()))
                player.playSound(player.location, Sound.BLOCK_ANVIL_USE, 1f, 1.5f)
            }
            UpgradeManager.UpgradeOutcome.FAIL_BREAK -> {
                itemSlot = null; resultItem = null
                player.sendMessage(mc.msgUpgradeBreak)
                player.playSound(player.location, Sound.BLOCK_ANVIL_BREAK, 1f, 0.8f)
            }
            UpgradeManager.UpgradeOutcome.FAIL_DOWN -> {
                resultItem = result.resultItem; itemSlot = null
                player.sendMessage(mc.format(mc.msgUpgradeDown,
                    "prev" to result.prevLevel.toString(), "new" to result.newLevel.toString()))
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 0.8f)
            }
            UpgradeManager.UpgradeOutcome.FAIL_KEEP -> {
                resultItem = result.resultItem; itemSlot = null
                player.sendMessage(mc.format(mc.msgUpgradeFail, "prev" to result.prevLevel.toString()))
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            }
        }

        // 강화 기록 저장
        rpg.upgradeHistoryManager.record(
            player.uniqueId, itemName, result.outcome, result.prevLevel, result.newLevel
        )

        // ItemUpgradeEvent 발행
        val upgradeEvent = ItemUpgradeEvent(
            player, result.resultItem ?: item, result.outcome, result.prevLevel, result.newLevel
        )
        org.bukkit.Bukkit.getPluginManager().callEvent(upgradeEvent)

        rerender()
    }

    private fun fmt(d: Double) = String.format("%.0f", d)

    private fun makeItem(mat: Material, name: String, lore: List<String> = emptyList()): ItemStack {
        val stack = ItemStack(mat)
        val meta: ItemMeta = stack.itemMeta
        meta.setDisplayName(name)
        if (lore.isNotEmpty()) meta.lore = lore
        stack.itemMeta = meta
        return stack
    }
}
