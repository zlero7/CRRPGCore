package io.zlero.cRRPGCore

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

object UpgradeGui : Listener {

    private const val SLOT_ITEM      = 11
    private const val SLOT_STONE     = 13
    private const val SLOT_BREAK_PRO = 20
    private const val SLOT_DOWN_PRO  = 21
    private const val SLOT_RESULT    = 15
    private val UPGRADE_BTN_SLOTS    = setOf(25, 26, 34, 35)

    private fun title() = CRRPGCorePlugin.plugin.msgCfg.guiUpgradeTitle

    private data class GuiState(
        var itemSlot:     ItemStack? = null,
        var stoneSlot:    ItemStack? = null,
        var breakProSlot: ItemStack? = null,
        var downProSlot:  ItemStack? = null,
        var resultItem:   ItemStack? = null
    )
    private val states = HashMap<java.util.UUID, GuiState>()

    fun open(player: Player) {
        states[player.uniqueId] = GuiState()
        val inv = Bukkit.createInventory(null, 36, title())
        buildGui(inv, GuiState())
        player.openInventory(inv)
    }

    private fun buildGui(inv: Inventory, state: GuiState) {
        val mc     = CRRPGCorePlugin.plugin.msgCfg
        val filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, "§r")
        for (i in 0 until 36) inv.setItem(i, filler)

        inv.setItem(SLOT_ITEM,      state.itemSlot     ?: makeItem(Material.LIME_STAINED_GLASS_PANE,   mc.guiUpgradeSlotItemName,  mc.guiUpgradeSlotItemLore))
        inv.setItem(SLOT_STONE,     state.stoneSlot    ?: makeItem(Material.YELLOW_STAINED_GLASS_PANE, mc.guiUpgradeSlotStoneName, mc.guiUpgradeSlotStoneLore))
        inv.setItem(SLOT_BREAK_PRO, state.breakProSlot ?: makeItem(Material.RED_STAINED_GLASS_PANE,    mc.guiUpgradeSlotBreakName, mc.guiUpgradeSlotBreakLore))
        inv.setItem(SLOT_DOWN_PRO,  state.downProSlot  ?: makeItem(Material.ORANGE_STAINED_GLASS_PANE, mc.guiUpgradeSlotDownName,  mc.guiUpgradeSlotDownLore))
        inv.setItem(SLOT_RESULT,    state.resultItem   ?: makeItem(Material.WHITE_STAINED_GLASS_PANE,  mc.guiUpgradeSlotResultName,mc.guiUpgradeSlotResultLore))
        refreshUpgradeButton(inv, state)
    }

    private fun refreshUpgradeButton(inv: Inventory, state: GuiState) {
        val plugin = CRRPGCorePlugin.plugin
        val mc     = plugin.msgCfg
        val upgMgr = plugin.upgradeManager
        val item   = state.itemSlot
        val stone  = state.stoneSlot

        val btnItem = when {
            item == null ->
                makeItem(Material.GRAY_STAINED_GLASS_PANE, mc.guiUpgradeBtnNoItem, mc.guiUpgradeBtnNoItemLore)

            !plugin.rpgItemManager.isRpgItem(item) ->
                makeItem(Material.BARRIER, mc.guiUpgradeBtnNotRpg, mc.guiUpgradeBtnNotRpgLore)

            upgMgr.getLevel(item) >= 10 ->
                makeItem(Material.NETHER_STAR, mc.guiUpgradeBtnMax, mc.guiUpgradeBtnMaxLore)

            else -> {
                val curLv    = upgMgr.getLevel(item)
                val tgtLv    = curLv + 1
                val chance   = upgMgr.getChance(tgtLv)
                val stoneOk  = stone != null && upgMgr.getStoneType(stone) == upgMgr.requiredStoneType(tgtLv)
                val hasBreak = upgMgr.isProtectBreak(state.breakProSlot)
                val hasDown  = upgMgr.isProtectDown(state.downProSlot)

                var dBreak = chance.breakPct
                var dDown  = chance.downPct
                val dSucc  = chance.successPct
                val dKeep  = chance.keepPct
                when {
                    hasBreak && hasDown -> { dBreak = 0.0; dDown = 0.0 }
                    hasBreak            -> { dBreak = 0.0 }
                    hasDown             -> { dDown  = 0.0 }
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
        UPGRADE_BTN_SLOTS.forEach { inv.setItem(it, btnItem) }
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (title(event.view) != title()) return

        event.isCancelled = true

        val player = event.whoClicked as? Player ?: return
        val plugin = CRRPGCorePlugin.plugin
        val mc     = plugin.msgCfg
        val upgMgr = plugin.upgradeManager
        val inv    = event.inventory
        val state  = states[player.uniqueId] ?: return

        val clickedInv = event.clickedInventory
        val slot       = event.slot

        if (clickedInv == player.inventory) {
            val clicked = event.currentItem
            if (clicked == null || clicked.type == Material.AIR) return

            val targetSlot = when {
                upgMgr.isProtectBreak(clicked)           -> SLOT_BREAK_PRO
                upgMgr.isProtectDown(clicked)            -> SLOT_DOWN_PRO
                upgMgr.isUpgradeStone(clicked)           -> SLOT_STONE
                plugin.rpgItemManager.isRpgItem(clicked) -> SLOT_ITEM
                else -> return
            }

            val existing = getStateSlot(state, targetSlot)
            if (existing != null) player.inventory.addItem(existing)

            val toPlace = clicked.clone().also { it.amount = 1 }
            setStateSlot(state, targetSlot, toPlace)

            if (clicked.amount > 1) clicked.amount -= 1
            else event.currentItem = null

            buildGui(inv, state)
            return
        }

        if (clickedInv != inv) return

        if (slot in UPGRADE_BTN_SLOTS) {
            if (event.isLeftClick) performUpgrade(player, inv, state, plugin, upgMgr)
            return
        }

        when (slot) {
            SLOT_ITEM -> { returnStateItem(player, state, SLOT_ITEM, inv, state.itemSlot) { state.itemSlot = null } }
            SLOT_STONE -> { returnStateItem(player, state, SLOT_STONE, inv, state.stoneSlot) { state.stoneSlot = null } }
            SLOT_BREAK_PRO -> { returnStateItem(player, state, SLOT_BREAK_PRO, inv, state.breakProSlot) { state.breakProSlot = null } }
            SLOT_DOWN_PRO -> { returnStateItem(player, state, SLOT_DOWN_PRO, inv, state.downProSlot) { state.downProSlot = null } }
            SLOT_RESULT -> {
                if (state.resultItem != null) {
                    player.inventory.addItem(state.resultItem!!)
                    state.resultItem = null
                    player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 1f, 1f)
                    buildGui(inv, state)
                }
            }
        }
    }

    private fun returnStateItem(player: Player, state: GuiState, slot: Int, inv: Inventory, item: ItemStack?, clear: () -> Unit) {
        if (item != null) {
            player.inventory.addItem(item)
            clear()
            buildGui(inv, state)
        }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (title(event.view) != title()) return
        event.isCancelled = true
    }

    private fun performUpgrade(
        player: Player, inv: Inventory, state: GuiState,
        plugin: CRRPGCorePlugin, upgMgr: UpgradeManager
    ) {
        val mc    = plugin.msgCfg
        val item  = state.itemSlot
        val stone = state.stoneSlot

        if (item == null)  { player.sendMessage(mc.errNeedItem); return }
        if (!plugin.rpgItemManager.isRpgItem(item)) { player.sendMessage(mc.errNotRpgItemUpgrade); return }
        if (stone == null) { player.sendMessage(mc.errNeedStone); return }

        val curLv = upgMgr.getLevel(item)
        if (curLv >= 10) { player.sendMessage(mc.errMaxUpgrade); return }

        val tgtLv = curLv + 1
        if (upgMgr.getStoneType(stone) != upgMgr.requiredStoneType(tgtLv)) {
            player.sendMessage(mc.format(mc.errWrongStone, "required" to upgMgr.requiredStoneName(tgtLv)))
            return
        }

        val hasBreak = upgMgr.isProtectBreak(state.breakProSlot)
        val hasDown  = upgMgr.isProtectDown(state.downProSlot)

        val result = upgMgr.tryUpgrade(item, hasBreak, hasDown) ?: return

        state.stoneSlot = null

        when (result.outcome) {
            UpgradeManager.UpgradeOutcome.FAIL_BREAK -> if (hasBreak) state.breakProSlot = null
            UpgradeManager.UpgradeOutcome.FAIL_DOWN  -> if (hasDown)  state.downProSlot  = null
            else -> {}
        }

        when (result.outcome) {
            UpgradeManager.UpgradeOutcome.SUCCESS -> {
                state.resultItem = item.clone(); state.itemSlot = null
                player.sendMessage(mc.format(mc.msgUpgradeSuccess,
                    "prev" to result.prevLevel.toString(), "new" to result.newLevel.toString()))
                player.playSound(player.location, Sound.BLOCK_ANVIL_USE, 1f, 1.5f)
            }
            UpgradeManager.UpgradeOutcome.FAIL_BREAK -> {
                state.itemSlot = null; state.resultItem = null
                player.sendMessage(mc.msgUpgradeBreak)
                player.playSound(player.location, Sound.BLOCK_ANVIL_BREAK, 1f, 0.8f)
            }
            UpgradeManager.UpgradeOutcome.FAIL_DOWN -> {
                state.resultItem = item.clone(); state.itemSlot = null
                player.sendMessage(mc.format(mc.msgUpgradeDown,
                    "prev" to result.prevLevel.toString(), "new" to result.newLevel.toString()))
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 0.8f)
            }
            UpgradeManager.UpgradeOutcome.FAIL_KEEP -> {
                state.resultItem = item.clone(); state.itemSlot = null
                player.sendMessage(mc.format(mc.msgUpgradeFail, "prev" to result.prevLevel.toString()))
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            }
        }

        buildGui(inv, state)
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        if (title(event.view) != title()) return
        val player = event.player as? Player ?: return
        val state  = states.remove(player.uniqueId) ?: return
        listOfNotNull(state.itemSlot, state.stoneSlot, state.breakProSlot,
            state.downProSlot, state.resultItem)
            .forEach { player.inventory.addItem(it) }
    }

    private fun getStateSlot(state: GuiState, slot: Int): ItemStack? = when (slot) {
        SLOT_ITEM      -> state.itemSlot
        SLOT_STONE     -> state.stoneSlot
        SLOT_BREAK_PRO -> state.breakProSlot
        SLOT_DOWN_PRO  -> state.downProSlot
        else           -> null
    }

    private fun setStateSlot(state: GuiState, slot: Int, item: ItemStack) {
        when (slot) {
            SLOT_ITEM      -> state.itemSlot      = item
            SLOT_STONE     -> state.stoneSlot     = item
            SLOT_BREAK_PRO -> state.breakProSlot  = item
            SLOT_DOWN_PRO  -> state.downProSlot   = item
        }
    }

    private fun fmt(d: Double) = String.format("%.0f", d)

    private fun title(view: org.bukkit.inventory.InventoryView): String =
        LegacyComponentSerializer.legacySection().serialize(view.title())

    private fun makeItem(mat: Material, name: String, lore: List<String> = emptyList()): ItemStack {
        val stack = ItemStack(mat)
        val meta: ItemMeta = stack.itemMeta
        meta.setDisplayName(name)
        if (lore.isNotEmpty()) meta.lore = lore
        stack.itemMeta = meta
        return stack
    }

    fun closeAll() {
        val uuids = ArrayList<java.util.UUID>(states.keys)
        states.clear()
        uuids.forEach { uuid ->
            val player = CRRPGCorePlugin.plugin.server.getPlayer(uuid) ?: return@forEach
            player.closeInventory()
        }
    }
}
