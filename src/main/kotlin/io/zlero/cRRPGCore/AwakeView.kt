package io.zlero.cRRPGCore

import io.zlero.cRFramework.view.View
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 각성/감정 GUI — CRFramework View 기반
 *
 * 상단 인벤토리(3행 27칸):
 *   - SLOT_WEAPON(11): 무기/보석 슬롯 — 클릭 시 아이템 반환
 *   - SLOT_SCROLL(13): 스크롤/각성석 슬롯 — 클릭 시 아이템 반환
 *   - SLOT_ACTION(15): 실행 버튼 — 상태에 따라 동적으로 표시, 클릭 시 각성/감정 처리
 * 하단 인벤토리 클릭    : GUIBottomClickListener → 스크롤 여부에 따라 알맞은 슬롯에 자동 배치
 *
 * 닫힘 시: weaponItem / scrollItem 인벤토리로 반환 (가득 찬 경우 바닥에 드롭)
 */
class AwakeView(private val rpg: CRRPGCorePlugin)
    : View(rpg, rpg.msgCfg.guiAwakeTitle, rows = 3) {

    var weaponItem: ItemStack? = null
    var scrollItem: ItemStack? = null

    companion object {
        const val SLOT_WEAPON = 11
        const val SLOT_SCROLL = 13
        const val SLOT_ACTION = 15

        private val openViews = ConcurrentHashMap<UUID, AwakeView>()

        fun openFor(rpg: CRRPGCorePlugin, player: Player) {
            val view = AwakeView(rpg)
            openViews[player.uniqueId] = view
            view.open(player)
        }

        fun isOpen(uuid: UUID) = openViews.containsKey(uuid)
        fun getView(uuid: UUID): AwakeView? = openViews[uuid]

        /** RpgCoreCommand에서 스크롤/각성석 아이템 생성 시 호출 */
        fun makeScrollItem(rpg: CRRPGCorePlugin, type: String): ItemStack? {
            val mc = rpg.msgCfg
            return when (type) {
                "scroll" -> {
                    val mat = runCatching { Material.valueOf(mc.scrollMaterial.uppercase()) }
                        .getOrDefault(Material.PAPER)
                    makeItemStatic(mat, mc.scrollName, mc.scrollLore).also { stack ->
                        val meta = stack.itemMeta!!
                        if (mc.scrollCustomModel > 0) meta.setCustomModelData(mc.scrollCustomModel)
                        stack.itemMeta = meta
                    }
                }
                "stone" -> {
                    val mat = runCatching { Material.valueOf(mc.stoneMaterial.uppercase()) }
                        .getOrDefault(Material.AMETHYST_SHARD)
                    makeItemStatic(mat, mc.stoneName, mc.stoneLore).also { stack ->
                        val meta = stack.itemMeta!!
                        if (mc.stoneCustomModel > 0) meta.setCustomModelData(mc.stoneCustomModel)
                        stack.itemMeta = meta
                    }
                }
                else -> null
            }
        }

        private fun makeItemStatic(mat: Material, name: String, lore: List<String> = emptyList()) =
            ItemStack(mat).apply {
                itemMeta = itemMeta?.also {
                    it.setDisplayName(name)
                    if (lore.isNotEmpty()) it.lore = lore
                }
            }
    }

    override fun io.zlero.cRFramework.view.scope.CreateScope.onCreate() {
        val mc = rpg.msgCfg

        // 배경 채우기
        fill(rows = 3) {
            item(makeItem(Material.GRAY_STAINED_GLASS_PANE, " "))
        }

        // 무기/보석 슬롯
        button(slot = SLOT_WEAPON) {
            item { _ ->
                weaponItem ?: makeItem(Material.ITEM_FRAME, mc.guiAwakeSlotWeapon, mc.guiAwakeSlotWeaponLore)
            }
            onClick { player ->
                val item = weaponItem ?: return@onClick
                weaponItem = null
                player.inventory.addItem(item).values.forEach { leftover ->
                    player.world.dropItem(player.location, leftover)
                }
                rerender()
            }
        }

        // 스크롤/각성석 슬롯
        button(slot = SLOT_SCROLL) {
            item { _ ->
                scrollItem ?: makeItem(Material.PAPER, mc.guiAwakeSlotScroll, mc.guiAwakeSlotScrollLore)
            }
            onClick { player ->
                val item = scrollItem ?: return@onClick
                scrollItem = null
                player.inventory.addItem(item).values.forEach { leftover ->
                    player.world.dropItem(player.location, leftover)
                }
                rerender()
            }
        }

        // 실행 버튼 (상태에 따라 동적 표시)
        button(slot = SLOT_ACTION) {
            item { player -> buildActionButton(player) }
            onClick { player -> processAwake(player) }
        }
    }

    override fun onClose(player: Player) {
        openViews.remove(player.uniqueId)
        listOfNotNull(weaponItem, scrollItem).forEach { item ->
            player.inventory.addItem(item).values.forEach { leftover ->
                player.world.dropItem(player.location, leftover)
            }
        }
        weaponItem = null
        scrollItem = null
    }

    // ─────────────────────────────────────────────────────────────────
    //  실행 버튼 아이템 동적 생성
    // ─────────────────────────────────────────────────────────────────
    private fun buildActionButton(player: Player): ItemStack {
        val mc     = rpg.msgCfg
        val weapon = weaponItem
        val scroll = scrollItem

        if (weapon == null || scroll == null)
            return makeItem(Material.GRAY_STAINED_GLASS_PANE, mc.guiAwakePending)

        val scrollName = scroll.itemMeta?.displayName
            ?: return makeItem(Material.GRAY_STAINED_GLASS_PANE, mc.guiAwakePending)

        val rpm = rpg.rpgItemManager
        val jm  = rpg.jewelManager
        val am  = rpg.appraisalManager
        val sm  = rpg.socketManager
        val eco = rpg.economy

        // 보석 감정
        if (jm.isJewel(weapon)) {
            val isAppraisal = scrollName == mc.scrollName
            if (!isAppraisal)            return makeItem(Material.BARRIER, mc.errJewelOnly)
            if (jm.isAppraised(weapon))  return makeItem(Material.BARRIER, mc.errJewelAlreadyApp)

            val cost      = am.appraisalCost
            val hasEnough = eco == null || eco.has(player, cost.toDouble())
            val mat       = if (hasEnough) Material.ENCHANTED_BOOK else Material.BARRIER
            val title     = if (hasEnough) mc.guiAwakeActionOk else mc.guiAwakeNoMoney
            return makeItem(mat, title, buildList {
                add("${mc.guiAwakeLabelAction}${mc.guiAwakeLabelAppraise}")
                add(mc.format(mc.guiAwakeLabelCost, "cost" to cost.toString()))
                if (!hasEnough) add(mc.guiAwakeLabelNoMoney) else add(mc.guiAwakeLabelOk)
            })
        }

        val gradeId = weapon.itemMeta?.persistentDataContainer
            ?.get(rpm.keyGrade, PersistentDataType.STRING)
        val grade   = gradeId?.let { ItemGrade.fromId(it) }
            ?: return makeItem(Material.BARRIER, mc.errAwakeNotRpgItem)

        val isAppraisal = scrollName == mc.scrollName
        val isAwake     = scrollName == mc.stoneName
        if (!isAppraisal && !isAwake) return makeItem(Material.BARRIER, mc.errAwakeWrongScroll)

        val cost = if (isAppraisal) {
            if (am.isAppraised(weapon)) am.appraisalRerollCost else am.appraisalCost
        } else {
            if (sm.hasSocket(weapon)) sm.socketRerollCost else sm.socketCost
        }
        val hasEnough   = eco == null || eco.has(player, cost.toDouble())
        val actionLabel = if (isAppraisal) {
            if (am.isAppraised(weapon)) mc.guiAwakeLabelReappraise else mc.guiAwakeLabelAppraise
        } else {
            if (sm.hasSocket(weapon)) mc.guiAwakeLabelReawake else mc.guiAwakeLabelAwake
        }

        val mat   = if (hasEnough) Material.ENCHANTED_BOOK else Material.BARRIER
        val title = if (hasEnough) mc.guiAwakeActionOk else mc.guiAwakeNoMoney
        return makeItem(mat, title, buildList {
            add("${mc.guiAwakeLabelAction}$actionLabel")
            add(mc.format(mc.guiAwakeLabelCost, "cost" to cost.toString()))
            if (!hasEnough) add(mc.guiAwakeLabelNoMoney) else add(mc.guiAwakeLabelOk)
        })
    }

    // ─────────────────────────────────────────────────────────────────
    //  각성/감정 처리
    // ─────────────────────────────────────────────────────────────────
    private fun processAwake(player: Player) {
        val mc = rpg.msgCfg
        val weapon = weaponItem ?: run { player.sendMessage(mc.msgAwakeSlotHint11); return }
        val scroll = scrollItem ?: run { player.sendMessage(mc.msgAwakeSlotHint13); return }
        val scrollName = scroll.itemMeta?.displayName
            ?: run { player.sendMessage(mc.errAwakeWrongScroll); return }

        val jm  = rpg.jewelManager
        val sm  = rpg.socketManager
        val am  = rpg.appraisalManager
        val rpm = rpg.rpgItemManager
        val eco = rpg.economy

        // 보석 감정
        if (jm.isJewel(weapon)) {
            if (scrollName != mc.scrollName) {
                player.sendMessage(mc.errJewelOnly); sm.playFail(player); return
            }
            if (jm.isAppraised(weapon)) {
                player.sendMessage(mc.errJewelAlreadyApp); sm.playFail(player); return
            }
            val cost = am.appraisalCost
            if (eco != null && !eco.has(player, cost.toDouble())) {
                player.sendMessage(mc.format(mc.errJewelNotEnoughMoney, "cost" to cost.toString()))
                sm.playFail(player); return
            }
            consumeScroll()
            eco?.withdrawPlayer(player, cost.toDouble())
            if (jm.appraise(weapon)) {
                player.sendMessage(mc.format(mc.msgJewelAppraisalOk, "cost" to cost.toString()))
                sm.playSuccess(player)
            } else {
                player.sendMessage(mc.errJewelAppraisalFail)
                sm.playFail(player)
            }
            rerender()
            return
        }

        val isAppraisal = scrollName == mc.scrollName
        val isAwake     = scrollName == mc.stoneName
        if (!isAppraisal && !isAwake) { player.sendMessage(mc.errAwakeWrongScroll); return }

        val gradeId = weapon.itemMeta?.persistentDataContainer
            ?.get(rpm.keyGrade, PersistentDataType.STRING)
        val grade = gradeId?.let { ItemGrade.fromId(it) }
            ?: run { player.sendMessage(mc.errAwakeNotRpgItem); return }

        val cost = if (isAppraisal) {
            if (am.isAppraised(weapon)) am.appraisalRerollCost else am.appraisalCost
        } else {
            if (sm.hasSocket(weapon)) sm.socketRerollCost else sm.socketCost
        }
        if (eco != null && !eco.has(player, cost.toDouble())) {
            player.sendMessage(mc.format(mc.errNotEnoughMoneySocket, "cost" to cost.toString()))
            sm.playFail(player); return
        }

        if (isAppraisal && !sm.hasSocket(weapon)) {
            player.sendMessage(mc.errNotAwakened); sm.playFail(player); return
        }
        if (isAppraisal && am.isAppraised(weapon)) {
            val pdc = weapon.itemMeta?.persistentDataContainer ?: return
            val cur = pdc.get(am.keyAppraisalRerollCnt, PersistentDataType.INTEGER) ?: 0
            val max = pdc.get(am.keyAppraisalMaxReroll,  PersistentDataType.INTEGER) ?: -1
            if (max != -1 && cur >= max) { player.sendMessage(mc.errAwakeApprMaxReached); sm.playFail(player); return }
        }
        if (!isAppraisal && sm.hasSocket(weapon)) {
            val pdc = weapon.itemMeta?.persistentDataContainer ?: return
            val cur = pdc.get(sm.keySocketRerollCnt, PersistentDataType.INTEGER) ?: 0
            val max = pdc.get(sm.keySocketMaxReroll,  PersistentDataType.INTEGER) ?: -1
            if (max != -1 && cur >= max) { player.sendMessage(mc.errAwakeSockMaxReached); sm.playFail(player); return }
        }

        consumeScroll()
        eco?.withdrawPlayer(player, cost.toDouble())

        if (isAppraisal) {
            if (!am.isAppraised(weapon)) {
                when (am.appraise(weapon, grade, rpm)) {
                    AppraisalManager.AppraisalResult.SUCCESS -> {
                        player.sendMessage(mc.format(mc.msgAwakeAppraisalOk, "cost" to cost.toString()))
                        sm.playSuccess(player)
                    }
                    AppraisalManager.AppraisalResult.NO_SOCKET -> {
                        player.sendMessage(mc.errNotAwakened); sm.playFail(player)
                    }
                    AppraisalManager.AppraisalResult.ALREADY_APPRAISED -> {
                        player.sendMessage(mc.errAlreadyAppraised); sm.playFail(player)
                    }
                    AppraisalManager.AppraisalResult.FAIL -> {
                        player.sendMessage(mc.errAppraisalFail); sm.playFail(player)
                    }
                }
            } else {
                when (am.rerollAppraisal(weapon, grade, rpm)) {
                    AppraisalManager.AppraisalRerollResult.SUCCESS -> {
                        player.sendMessage(mc.format(mc.msgAwakeReappraisalOk, "cost" to cost.toString()))
                        sm.playReroll(player)
                    }
                    AppraisalManager.AppraisalRerollResult.MAX_REACHED -> {
                        player.sendMessage(mc.errAwakeApprMaxReached); sm.playFail(player)
                    }
                    else -> { player.sendMessage(mc.errAppraisalFail); sm.playFail(player) }
                }
            }
        } else {
            if (!sm.hasSocket(weapon)) {
                when (sm.applySocket(weapon, grade)) {
                    SocketManager.SocketResult.SUCCESS -> {
                        val cnt = sm.getSocketCount(weapon)
                        player.sendMessage(mc.format(mc.msgAwakeSocketOk,
                            "slots" to cnt.toString(), "cost" to cost.toString()))
                        sm.playSuccess(player)
                    }
                    SocketManager.SocketResult.ALREADY_HAS_SOCKET -> {
                        player.sendMessage(mc.errAlreadyAwakened); sm.playFail(player)
                    }
                    SocketManager.SocketResult.FAIL_NO_META -> {
                        player.sendMessage(mc.errAppraisalFail); sm.playFail(player)
                    }
                }
            } else {
                when (sm.rerollSocket(weapon, grade)) {
                    SocketManager.SocketRerollResult.SUCCESS -> {
                        val meta = weapon.itemMeta
                        if (meta != null) {
                            meta.persistentDataContainer.remove(am.keyAppraised)
                            meta.persistentDataContainer.remove(am.keyAppraisalRerollCnt)
                            weapon.itemMeta = meta
                        }
                        sm.refreshLoreAfterSocketReroll(weapon, grade, am)
                        val newCount = sm.getSocketCount(weapon)
                        player.sendMessage(mc.format(mc.msgAwakeSocketRerollOk,
                            "slots" to newCount.toString(), "cost" to cost.toString()))
                        sm.playReroll(player)
                    }
                    SocketManager.SocketRerollResult.MAX_REACHED -> {
                        player.sendMessage(mc.errAwakeSockMaxReached); sm.playFail(player)
                    }
                    else -> { player.sendMessage(mc.errAppraisalFail); sm.playFail(player) }
                }
            }
        }

        rerender()
    }

    private fun consumeScroll() {
        val scroll = scrollItem ?: return
        if (scroll.amount <= 1) scrollItem = null
        else scroll.amount -= 1
    }

    private fun makeItem(mat: Material, name: String, lore: List<String> = emptyList()) =
        ItemStack(mat).apply {
            itemMeta = itemMeta?.also {
                it.setDisplayName(name)
                if (lore.isNotEmpty()) it.lore = lore
            }
        }
}
