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
 * 상단 인벤토리(1행 9칸): 버튼 클릭 → 장착된 루 해제
 * 하단 인벤토리 클릭    : GUIBottomClickListener → isJewel 검증 후 빈 슬롯에 자동 장착
 *
 * 닫힘 시: saveSlots() + 1틱 지연 후 armorHealth 적용
 */
class RoonView(private val rpg: CRRPGCorePlugin)
    : View(rpg, rpg.msgCfg.guiRoonTitle, rows = 1) {

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
                    rpg.jewelManager.getSlots(player)[slotIndex] ?: emptySlot(slotIndex + 1)
                }
                onClick { player ->
                    val jewel = rpg.jewelManager.getSlots(player)[slotIndex] ?: return@onClick
                    rpg.jewelManager.setSlot(player, slotIndex, null)
                    player.inventory.addItem(jewel)
                    player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 1f, 1f)
                    player.sendMessage(
                        rpg.msgCfg.format(rpg.msgCfg.msgRoonUnequip, "slot" to (slotIndex + 1).toString())
                    )
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
}
