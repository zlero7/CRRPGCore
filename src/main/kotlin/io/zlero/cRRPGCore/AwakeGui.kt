package io.zlero.cRRPGCore

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object AwakeGui : Listener {

    private val openInventories = mutableSetOf<Inventory>()

    private const val SLOT_WEAPON = 11
    private const val SLOT_SCROLL = 13
    private const val SLOT_ACTION = 15

    private fun title() = CRRPGCorePlugin.plugin.msgCfg.guiAwakeTitle

    fun open(player: Player) {
        val inv = Bukkit.createInventory(null, 27, title())
        fillBorder(inv)
        openInventories.add(inv)
        player.openInventory(inv)
    }

    private fun fillBorder(inv: Inventory) {
        val mc    = CRRPGCorePlugin.plugin.msgCfg
        val glass = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ")
        for (i in 0 until 27) {
            if (i !in listOf(SLOT_WEAPON, SLOT_SCROLL, SLOT_ACTION)) inv.setItem(i, glass)
        }
        inv.setItem(SLOT_WEAPON, makeItem(Material.ITEM_FRAME, mc.guiAwakeSlotWeapon, mc.guiAwakeSlotWeaponLore))
        inv.setItem(SLOT_SCROLL, makeItem(Material.PAPER,      mc.guiAwakeSlotScroll, mc.guiAwakeSlotScrollLore))
        setResultPending(inv)
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val inv = event.inventory
        if (!openInventories.contains(inv)) return

        val player     = event.whoClicked as? Player ?: return
        val clickedInv = event.clickedInventory
        val rawSlot    = event.rawSlot

        if (clickedInv == inv) {
            when (rawSlot) {
                SLOT_WEAPON, SLOT_SCROLL -> {
                    val current = inv.getItem(rawSlot)
                    if (current != null && isGuideItem(current)) inv.setItem(rawSlot, null)

                    if (event.click == ClickType.SHIFT_LEFT || event.click == ClickType.SHIFT_RIGHT) {
                        event.isCancelled = true
                        val slotItem = inv.getItem(rawSlot)
                        if (slotItem != null && !isGuideItem(slotItem)) {
                            player.inventory.addItem(slotItem).values.forEach {
                                player.world.dropItem(player.location, it)
                            }
                            inv.setItem(rawSlot, null)
                        }
                    }
                    scheduleUpdate(inv, player)
                }
                SLOT_ACTION -> {
                    event.isCancelled = true
                    processAwake(inv, player)
                }
                else -> event.isCancelled = true
            }
            return
        }

        if (event.click == ClickType.SHIFT_LEFT || event.click == ClickType.SHIFT_RIGHT) {
            event.isCancelled = true
            val item = event.currentItem?.takeIf { it.type != Material.AIR } ?: return

            val targetSlot = if (isScrollItem(item)) SLOT_SCROLL else SLOT_WEAPON
            val current    = inv.getItem(targetSlot)

            if (current == null || isGuideItem(current)) {
                inv.setItem(targetSlot, item.clone())
                event.currentItem = ItemStack(Material.AIR)
                scheduleUpdate(inv, player)
            }
        }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        val inv = event.inventory
        if (!openInventories.contains(inv)) return
        val guiSlots = event.rawSlots.filter { it < 27 }
        if (guiSlots.isNotEmpty() && !guiSlots.all { it == SLOT_WEAPON || it == SLOT_SCROLL }) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val inv = event.inventory
        if (!openInventories.contains(inv)) return
        openInventories.remove(inv)

        val player = event.player as? Player ?: return
        listOf(SLOT_WEAPON, SLOT_SCROLL).forEach { slot ->
            val item = inv.getItem(slot)
            if (item != null && !isGuideItem(item)) {
                player.inventory.addItem(item).values.forEach { leftover ->
                    player.world.dropItem(player.location, leftover)
                }
                inv.setItem(slot, null)
            }
        }
    }

    private fun scheduleUpdate(inv: Inventory, player: Player) {
        Bukkit.getScheduler().runTaskLater(
            CRRPGCorePlugin.plugin, Runnable { updateResultSlot(inv, player) }, 1L
        )
    }

    private fun updateResultSlot(inv: Inventory, player: Player) {
        val plugin = CRRPGCorePlugin.plugin
        val mc     = plugin.msgCfg

        val weaponItem = getRealItem(inv, SLOT_WEAPON) ?: run { setResultPending(inv); return }
        val scrollItem = getRealItem(inv, SLOT_SCROLL) ?: run { setResultPending(inv); return }
        val scrollName = scrollItem.itemMeta?.displayName ?: run { setResultPending(inv); return }

        val rpm = plugin.rpgItemManager
        val jm  = plugin.jewelManager

        if (jm.isJewel(weaponItem)) {
            val isAppraisal = scrollName == mc.scrollName
            if (!isAppraisal) { setResultError(inv, mc.errJewelOnly); return }
            if (jm.isAppraised(weaponItem)) { setResultError(inv, mc.errJewelAlreadyApp); return }

            val eco  = plugin.economy
            val cost = plugin.appraisalManager.appraisalCost
            val hasEnough = eco == null || eco.has(player, cost.toDouble())
            val mat   = if (hasEnough) Material.ENCHANTED_BOOK else Material.BARRIER
            val title = if (hasEnough) mc.guiAwakeActionOk else mc.guiAwakeNoMoney
            inv.setItem(SLOT_ACTION, makeItem(mat, title, buildList {
                add("${mc.guiAwakeLabelAction}${mc.guiAwakeLabelAppraise}")
                add(mc.format(mc.guiAwakeLabelCost, "cost" to cost.toString()))
                if (!hasEnough) add(mc.guiAwakeLabelNoMoney) else add(mc.guiAwakeLabelOk)
            }))
            return
        }

        val gradeId = weaponItem.itemMeta?.persistentDataContainer
            ?.get(rpm.keyGrade, PersistentDataType.STRING)
        val grade   = gradeId?.let { ItemGrade.fromId(it) }
            ?: run { setResultError(inv, mc.errAwakeNotRpgItem); return }

        val am = plugin.appraisalManager
        val sm = plugin.socketManager

        val isAppraisal = scrollName == mc.scrollName
        val isAwake     = scrollName == mc.stoneName
        if (!isAppraisal && !isAwake) { setResultError(inv, mc.errAwakeWrongScroll); return }

        val eco  = plugin.economy
        val cost = if (isAppraisal) {
            if (am.isAppraised(weaponItem)) am.appraisalRerollCost else am.appraisalCost
        } else {
            if (sm.hasSocket(weaponItem)) sm.socketRerollCost else sm.socketCost
        }
        val hasEnough   = eco == null || eco.has(player, cost.toDouble())
        val actionLabel = if (isAppraisal) {
            if (am.isAppraised(weaponItem)) mc.guiAwakeLabelReappraise else mc.guiAwakeLabelAppraise
        } else {
            if (sm.hasSocket(weaponItem)) mc.guiAwakeLabelReawake else mc.guiAwakeLabelAwake
        }

        val mat   = if (hasEnough) Material.ENCHANTED_BOOK else Material.BARRIER
        val title = if (hasEnough) mc.guiAwakeActionOk else mc.guiAwakeNoMoney
        inv.setItem(SLOT_ACTION, makeItem(mat, title, buildList {
            add("${mc.guiAwakeLabelAction}$actionLabel")
            add(mc.format(mc.guiAwakeLabelCost, "cost" to cost.toString()))
            if (!hasEnough) add(mc.guiAwakeLabelNoMoney) else add(mc.guiAwakeLabelOk)
        }))
    }

    private fun processAwake(inv: Inventory, player: Player) {
        val plugin     = CRRPGCorePlugin.plugin
        val mc         = plugin.msgCfg
        val weaponItem = getRealItem(inv, SLOT_WEAPON)
            ?: run { player.sendMessage(mc.msgAwakeSlotHint11); return }
        val scrollItem = getRealItem(inv, SLOT_SCROLL)
            ?: run { player.sendMessage(mc.msgAwakeSlotHint13); return }
        val scrollName = scrollItem.itemMeta?.displayName
            ?: run { player.sendMessage(mc.errAwakeWrongScroll); return }

        val jm  = plugin.jewelManager
        val sm  = plugin.socketManager
        val am  = plugin.appraisalManager
        val rpm = plugin.rpgItemManager
        val eco = plugin.economy

        // 보석 감정
        if (jm.isJewel(weaponItem)) {
            if (scrollName != mc.scrollName) {
                player.sendMessage(mc.errJewelOnly)
                sm.playFail(player)
                return
            }
            if (jm.isAppraised(weaponItem)) {
                player.sendMessage(mc.errJewelAlreadyApp)
                sm.playFail(player)
                return
            }
            val cost = am.appraisalCost
            if (eco != null && !eco.has(player, cost.toDouble())) {
                player.sendMessage(mc.format(mc.errJewelNotEnoughMoney, "cost" to cost.toString()))
                sm.playFail(player)
                return
            }
            consumeScroll(inv, scrollItem)
            eco?.withdrawPlayer(player, cost.toDouble())

            if (jm.appraise(weaponItem)) {
                player.sendMessage(mc.format(mc.msgJewelAppraisalOk, "cost" to cost.toString()))
                sm.playSuccess(player)
                inv.setItem(SLOT_WEAPON, weaponItem)
            } else {
                player.sendMessage(mc.errJewelAppraisalFail)
                sm.playFail(player)
            }
            updateResultSlot(inv, player)
            return
        }

        val isAppraisal = scrollName == mc.scrollName
        val isAwake     = scrollName == mc.stoneName
        if (!isAppraisal && !isAwake) {
            player.sendMessage(mc.errAwakeWrongScroll)
            return
        }

        val gradeId = weaponItem.itemMeta?.persistentDataContainer
            ?.get(rpm.keyGrade, PersistentDataType.STRING)
        val grade   = gradeId?.let { ItemGrade.fromId(it) }
            ?: run { player.sendMessage(mc.errAwakeNotRpgItem); return }

        val cost = if (isAppraisal) {
            if (am.isAppraised(weaponItem)) am.appraisalRerollCost else am.appraisalCost
        } else {
            if (sm.hasSocket(weaponItem)) sm.socketRerollCost else sm.socketCost
        }

        if (eco != null && !eco.has(player, cost.toDouble())) {
            player.sendMessage(mc.format(mc.errNotEnoughMoneySocket, "cost" to cost.toString()))
            sm.playFail(player)
            return
        }

        if (isAppraisal && !sm.hasSocket(weaponItem)) {
            player.sendMessage(mc.errNotAwakened); sm.playFail(player); return
        }
        if (isAppraisal && am.isAppraised(weaponItem)) {
            val pdc = weaponItem.itemMeta?.persistentDataContainer ?: return
            val cur = pdc.get(am.keyAppraisalRerollCnt, PersistentDataType.INTEGER) ?: 0
            val max = pdc.get(am.keyAppraisalMaxReroll,  PersistentDataType.INTEGER) ?: -1
            if (max != -1 && cur >= max) { player.sendMessage(mc.errAwakeApprMaxReached); sm.playFail(player); return }
        }
        if (!isAppraisal && sm.hasSocket(weaponItem)) {
            val pdc = weaponItem.itemMeta?.persistentDataContainer ?: return
            val cur = pdc.get(sm.keySocketRerollCnt, PersistentDataType.INTEGER) ?: 0
            val max = pdc.get(sm.keySocketMaxReroll,  PersistentDataType.INTEGER) ?: -1
            if (max != -1 && cur >= max) { player.sendMessage(mc.errAwakeSockMaxReached); sm.playFail(player); return }
        }

        consumeScroll(inv, scrollItem)
        eco?.withdrawPlayer(player, cost.toDouble())

        if (isAppraisal) {
            if (!am.isAppraised(weaponItem)) {
                when (am.appraise(weaponItem, grade, rpm)) {
                    AppraisalManager.AppraisalResult.SUCCESS -> {
                        player.sendMessage(mc.format(mc.msgAwakeAppraisalOk, "cost" to cost.toString()))
                        sm.playSuccess(player); inv.setItem(SLOT_WEAPON, weaponItem)
                    }
                    AppraisalManager.AppraisalResult.NO_SOCKET -> { player.sendMessage(mc.errNotAwakened); sm.playFail(player) }
                    AppraisalManager.AppraisalResult.ALREADY_APPRAISED -> { player.sendMessage(mc.errAlreadyAppraised); sm.playFail(player) }
                    AppraisalManager.AppraisalResult.FAIL -> { player.sendMessage(mc.errAppraisalFail); sm.playFail(player) }
                }
            } else {
                when (am.rerollAppraisal(weaponItem, grade, rpm)) {
                    AppraisalManager.AppraisalRerollResult.SUCCESS -> {
                        player.sendMessage(mc.format(mc.msgAwakeReappraisalOk, "cost" to cost.toString()))
                        sm.playReroll(player); inv.setItem(SLOT_WEAPON, weaponItem)
                    }
                    AppraisalManager.AppraisalRerollResult.MAX_REACHED -> { player.sendMessage(mc.errAwakeApprMaxReached); sm.playFail(player) }
                    else -> { player.sendMessage(mc.errAppraisalFail); sm.playFail(player) }
                }
            }
        } else {
            if (!sm.hasSocket(weaponItem)) {
                when (sm.applySocket(weaponItem, grade)) {
                    SocketManager.SocketResult.SUCCESS -> {
                        val cnt = sm.getSocketCount(weaponItem)
                        player.sendMessage(mc.format(mc.msgAwakeSocketOk, "slots" to cnt.toString(), "cost" to cost.toString()))
                        sm.playSuccess(player); inv.setItem(SLOT_WEAPON, weaponItem)
                    }
                    SocketManager.SocketResult.ALREADY_HAS_SOCKET -> { player.sendMessage(mc.errAlreadyAwakened); sm.playFail(player) }
                    SocketManager.SocketResult.FAIL_NO_META -> { player.sendMessage(mc.errAppraisalFail); sm.playFail(player) }
                }
            } else {
                when (sm.rerollSocket(weaponItem, grade)) {
                    SocketManager.SocketRerollResult.SUCCESS -> {
                        val meta = weaponItem.itemMeta
                        if (meta != null) {
                            meta.persistentDataContainer.remove(am.keyAppraised)
                            meta.persistentDataContainer.remove(am.keyAppraisalRerollCnt)
                            weaponItem.itemMeta = meta
                        }
                        sm.refreshLoreAfterSocketReroll(weaponItem, grade, am)
                        val newCount = sm.getSocketCount(weaponItem)
                        player.sendMessage(mc.format(mc.msgAwakeSocketRerollOk, "slots" to newCount.toString(), "cost" to cost.toString()))
                        sm.playReroll(player); inv.setItem(SLOT_WEAPON, weaponItem)
                    }
                    SocketManager.SocketRerollResult.MAX_REACHED -> { player.sendMessage(mc.errAwakeSockMaxReached); sm.playFail(player) }
                    else -> { player.sendMessage(mc.errAppraisalFail); sm.playFail(player) }
                }
            }
        }

        updateResultSlot(inv, player)
    }

    fun makeScrollItem(type: String): ItemStack? {
        val mc = CRRPGCorePlugin.plugin.msgCfg
        return when (type) {
            "scroll" -> {
                val mat = runCatching { Material.valueOf(mc.scrollMaterial.uppercase()) }.getOrDefault(Material.PAPER)
                makeItem(mat, mc.scrollName, mc.scrollLore).also { stack ->
                    val meta = stack.itemMeta!!
                    if (mc.scrollCustomModel > 0) meta.setCustomModelData(mc.scrollCustomModel)
                    stack.itemMeta = meta
                }
            }
            "stone"  -> {
                val mat = runCatching { Material.valueOf(mc.stoneMaterial.uppercase()) }.getOrDefault(Material.AMETHYST_SHARD)
                makeItem(mat, mc.stoneName, mc.stoneLore).also { stack ->
                    val meta = stack.itemMeta!!
                    if (mc.stoneCustomModel > 0) meta.setCustomModelData(mc.stoneCustomModel)
                    stack.itemMeta = meta
                }
            }
            else -> null
        }
    }

    private fun isScrollItem(item: ItemStack): Boolean {
        val mc   = CRRPGCorePlugin.plugin.msgCfg
        val name = item.itemMeta?.displayName ?: return false
        return name == mc.scrollName || name == mc.stoneName
    }

    private fun isGuideItem(item: ItemStack): Boolean {
        val mc = CRRPGCorePlugin.plugin.msgCfg
        if (item.type == Material.GRAY_STAINED_GLASS_PANE) return true
        if (item.type == Material.ITEM_FRAME) return true
        val name = item.itemMeta?.displayName ?: return false
        return name in listOf(
            mc.guiAwakeSlotWeapon, mc.guiAwakeSlotScroll,
            mc.guiAwakePending, mc.guiAwakeNoMoney, mc.guiAwakeActionOk, " "
        )
    }

    private fun getRealItem(inv: Inventory, slot: Int): ItemStack? {
        val item = inv.getItem(slot) ?: return null
        return if (isGuideItem(item)) null else item
    }

    private fun setResultPending(inv: Inventory) {
        val mc = CRRPGCorePlugin.plugin.msgCfg
        inv.setItem(SLOT_ACTION, makeItem(Material.GRAY_STAINED_GLASS_PANE, mc.guiAwakePending))
    }

    private fun setResultError(inv: Inventory, msg: String) {
        inv.setItem(SLOT_ACTION, makeItem(Material.BARRIER, msg))
    }

    private fun consumeScroll(inv: Inventory, scrollItem: ItemStack) {
        if (scrollItem.amount <= 1) inv.setItem(SLOT_SCROLL, null)
        else { scrollItem.amount -= 1; inv.setItem(SLOT_SCROLL, scrollItem) }
    }

    private fun makeItem(mat: Material, name: String, lore: List<String> = emptyList()) =
        ItemStack(mat).apply {
            itemMeta = itemMeta?.also {
                it.setDisplayName(name)
                if (lore.isNotEmpty()) it.lore = lore
            }
        }
}
