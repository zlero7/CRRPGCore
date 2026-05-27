package io.zlero.cRRPGCore.listener

import io.zlero.cRRPGCore.CRRPGCorePlugin
import io.zlero.cRFramework.listener.annotation.Subscribe
import io.zlero.cRRPGCore.view.JewelCombineView
import io.zlero.cRRPGCore.view.RoonView
import io.zlero.cRRPGCore.view.AwakeView
import io.zlero.cRRPGCore.view.UpgradeView
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

/**
 * 하단(플레이어) 인벤토리 클릭 라우터
 *
 * CRFramework ViewListener는 상단 인벤토리 클릭만 View로 라우팅하고,
 * 모든 클릭에 isCancelled = true를 설정합니다.
 *
 * 이 리스너는 HIGH 우선순위로 하단 인벤토리 클릭을 가로채어
 * 열려 있는 GUI(RoonView / UpgradeView / AwakeView)에 아이템을 배치합니다.
 */
class GUIBottomClickListener(private val rpg: CRRPGCorePlugin) {

    @Subscribe(priority = EventPriority.HIGH)
    fun onBottomClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val uuid   = player.uniqueId

        // 하단(플레이어) 인벤토리 클릭만 처리
        val clickedInv = event.clickedInventory ?: return
        if (clickedInv != player.inventory) return

        val item = event.currentItem?.takeIf { it.type != Material.AIR } ?: return

        when {
            RoonView.isOpen(uuid)         -> handleRoon(event, player, item)
            UpgradeView.isOpen(uuid)      -> handleUpgrade(event, player, item)
            AwakeView.isOpen(uuid)        -> handleAwake(event, player, item)
            JewelCombineView.isOpen(uuid) -> handleJewelCombine(event, player, item)
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  RoonView: 보석 → 빈 루 슬롯에 장착
    // ─────────────────────────────────────────────────────────────────
    private fun handleRoon(event: InventoryClickEvent, player: Player, item: ItemStack) {
        val mc  = rpg.msgCfg
        val mgr = rpg.jewelManager

        if (!mgr.isJewel(item)) {
            event.isCancelled = true
            player.sendMessage(mc.errJewelOnly2)
            return
        }
        if (!mgr.isAppraised(item)) {
            event.isCancelled = true
            player.sendMessage(mc.errNotAppraisedJewel)
            return
        }

        val view  = RoonView.getView(player.uniqueId)
        val slots = mgr.getSlots(player)

        // 지정 슬롯이 있으면 우선 사용, 없으면 첫 빈 슬롯
        val target = view?.targetSlot?.takeIf { slots[it] == null }
            ?: slots.indexOfFirst { it == null }

        if (target < 0) {
            event.isCancelled = true
            player.sendMessage(mc.errRoonFull)
            return
        }

        event.isCancelled = true

        val toPlace = item.clone().also { it.amount = 1 }
        mgr.setSlot(player, target, toPlace)
        if (item.amount > 1) item.amount -= 1 else event.currentItem = null

        // 지정 슬롯 선택 해제
        if (view != null) view.targetSlot = null

        player.playSound(player.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f)
        player.sendMessage(mc.format(mc.msgRoonEquip, "slot" to (target + 1).toString()))

        view?.rerender()
    }

    // ─────────────────────────────────────────────────────────────────
    //  UpgradeView: 아이템 종류에 따라 알맞은 슬롯에 배치
    // ─────────────────────────────────────────────────────────────────
    private fun handleUpgrade(event: InventoryClickEvent, player: Player, item: ItemStack) {
        val view   = UpgradeView.getView(player.uniqueId) ?: return
        val upgMgr = rpg.upgradeManager
        val rpm    = rpg.rpgItemManager

        val targetSlot = when {
            upgMgr.isProtectBreak(item) -> "break"
            upgMgr.isProtectDown(item)  -> "down"
            upgMgr.isUpgradeStone(item) -> "stone"
            rpm.isRpgItem(item)         -> "item"
            else                        -> return  // 인식할 수 없는 아이템 → 취소하지 않고 통과
        }

        event.isCancelled = true

        // 기존 슬롯 아이템 반환
        val existing = when (targetSlot) {
            "break" -> view.breakProSlot
            "down"  -> view.downProSlot
            "stone" -> view.stoneSlot
            else    -> view.itemSlot
        }
        if (existing != null) player.inventory.addItem(existing)

        // 새 아이템 배치
        val toPlace = item.clone().also { it.amount = 1 }
        when (targetSlot) {
            "break" -> view.breakProSlot = toPlace
            "down"  -> view.downProSlot  = toPlace
            "stone" -> view.stoneSlot    = toPlace
            else    -> view.itemSlot     = toPlace
        }

        if (item.amount > 1) item.amount -= 1 else event.currentItem = null
        view.rerender()
    }

    // ─────────────────────────────────────────────────────────────────
    //  JewelCombineView: 보석 → 재료 슬롯에 배치
    // ─────────────────────────────────────────────────────────────────
    private fun handleJewelCombine(event: InventoryClickEvent, player: Player, item: ItemStack) {
        val view = JewelCombineView.getView(player.uniqueId) ?: return
        val jm   = rpg.jewelManager
        if (!jm.isJewel(item)) {
            event.isCancelled = true
            player.sendMessage(rpg.msgCfg.errJewelOnly2)
            return
        }
        val emptyIdx = view.materialSlots.indexOfFirst { it == null }
        if (emptyIdx < 0) {
            event.isCancelled = true
            player.sendMessage("§c[!] 재료 슬롯이 가득 찼습니다.")
            return
        }
        event.isCancelled = true
        val toPlace = item.clone().also { it.amount = 1 }
        view.materialSlots[emptyIdx] = toPlace
        if (item.amount > 1) item.amount -= 1 else event.currentItem = null
        view.rerender()
    }

    // ─────────────────────────────────────────────────────────────────
    //  AwakeView: 스크롤 여부에 따라 스크롤 슬롯 또는 무기 슬롯에 배치
    // ─────────────────────────────────────────────────────────────────
    private fun handleAwake(event: InventoryClickEvent, player: Player, item: ItemStack) {
        val view = AwakeView.getView(player.uniqueId) ?: return
        val mc   = rpg.msgCfg

        val isScroll = item.itemMeta?.displayName?.let {
            it == mc.scrollName || it == mc.stoneName
        } ?: false

        event.isCancelled = true

        // 기존 슬롯 아이템 반환
        val existing = if (isScroll) view.scrollItem else view.weaponItem
        if (existing != null) player.inventory.addItem(existing)

        // 새 아이템 배치
        val toPlace = item.clone().also { it.amount = 1 }
        if (isScroll) view.scrollItem = toPlace else view.weaponItem = toPlace

        if (item.amount > 1) item.amount -= 1 else event.currentItem = null
        view.rerender()
    }
}
