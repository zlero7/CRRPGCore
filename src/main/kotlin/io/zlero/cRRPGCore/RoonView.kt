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
 * 루(Roon) 장착 GUI — CRFramework View 기반
 *
 * 상단 인벤토리(1행 9칸):
 *   - 장착된 루 클릭  → 해제
 *   - 빈 슬롯 클릭   → 해당 슬롯 선택(토글). 선택된 슬롯에 하단 인벤토리에서 보석 클릭 시 지정 장착
 * 하단 인벤토리 클릭  : GUIBottomClickListener → isJewel 검증 후 선택 슬롯(없으면 첫 빈 슬롯)에 장착
 *
 * 닫힘 시: saveSlots() + 1틱 지연 후 armorHealth 적용
 */
class RoonView(private val rpg: CRRPGCorePlugin)
    : View(rpg, rpg.msgCfg.guiRoonTitle, rows = 1) {

    /** 플레이어가 지정한 장착 대상 슬롯 (null = 미선택 → 첫 빈 슬롯에 자동 장착) */
    var targetSlot: Int? = null

    companion object {
        private val openViews = ConcurrentHashMap<UUID, RoonView>()

        fun openFor(rpg: CRRPGCorePlugin, player: Player) {
            val view = RoonView(rpg)
            openViews[player.uniqueId] = view
            view.open(player)
        }

        fun isOpen(uuid: UUID) = openViews.containsKey(uuid)
        fun getView(uuid: UUID): RoonView? = openViews[uuid]
    }

    override fun io.zlero.cRFramework.view.scope.CreateScope.onCreate() {
        for (i in 0..8) {
            val slotIndex = i
            button(slot = slotIndex) {
                item { player ->
                    val jewel = rpg.jewelManager.getSlots(player)[slotIndex]
                    when {
                        jewel != null           -> jewel
                        targetSlot == slotIndex -> selectedEmptySlot(slotIndex + 1)
                        else                    -> emptySlot(slotIndex + 1)
                    }
                }
                onClick { player ->
                    val jewel = rpg.jewelManager.getSlots(player)[slotIndex]
                    if (jewel != null) {
                        // 장착된 루 해제
                        rpg.jewelManager.setSlot(player, slotIndex, null)
                        player.inventory.addItem(jewel)
                        player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 1f, 1f)
                        player.sendMessage(
                            rpg.msgCfg.format(rpg.msgCfg.msgRoonUnequip, "slot" to (slotIndex + 1).toString())
                        )
                        if (targetSlot == slotIndex) targetSlot = null
                    } else {
                        // 빈 슬롯 클릭 → 선택 토글
                        targetSlot = if (targetSlot == slotIndex) null else slotIndex
                        if (targetSlot == slotIndex)
                            player.sendMessage("§a[룬] §f${slotIndex + 1}번 슬롯을 선택했습니다. 보석을 클릭하여 장착하세요.")
                        else
                            player.sendMessage("§7[룬] §f슬롯 선택을 해제했습니다.")
                    }
                    rerender()
                }
            }
        }
    }

    override fun onClose(player: Player) {
        openViews.remove(player.uniqueId)
        rpg.jewelManager.saveSlots(player)
        org.bukkit.Bukkit.getScheduler().runTaskLater(rpg, Runnable {
            rpg.armorHealthManager.applyArmorHealth(player)
        }, 1L)
    }

    private fun emptySlot(number: Int): ItemStack {
        val mc    = rpg.msgCfg
        val stack = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val meta: ItemMeta = stack.itemMeta
        meta.setDisplayName(mc.guiRoonEmptySlotName.replace("{number}", number.toString()))
        meta.lore = mc.guiRoonEmptySlotLore
        stack.itemMeta = meta
        return stack
    }

    /** 선택된 빈 슬롯 (LIME 유리, 클릭 안내 로어 포함) */
    private fun selectedEmptySlot(number: Int): ItemStack {
        val stack = ItemStack(Material.LIME_STAINED_GLASS_PANE)
        val meta: ItemMeta = stack.itemMeta
        meta.setDisplayName("§a▶ §f${number}번 슬롯 §a(선택됨)")
        meta.lore = listOf("§r", "  §7인벤토리에서 보석을 클릭하면", "  §e이 슬롯에 바로 장착됩니다.", "§r", "  §7다시 클릭하면 선택 해제")
        stack.itemMeta = meta
        return stack
    }
}
