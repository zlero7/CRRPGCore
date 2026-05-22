package io.zlero.cRRPGCore

import io.zlero.cRFramework.view.View
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class JewelCombineView(private val rpg: CRRPGCorePlugin)
    : View(rpg, "§8⚗ §5보석 합성", rows = 3) {

    val materialSlots = arrayOfNulls<ItemStack>(6)  // 슬롯 0~5
    var resultItem: ItemStack? = null

    companion object {
        val MATERIAL_SLOT_INDICES = listOf(0, 1, 2, 3, 4, 5)
        const val SLOT_RESULT  = 13
        const val SLOT_COMBINE = 22

        private val openViews = ConcurrentHashMap<UUID, JewelCombineView>()

        fun openFor(rpg: CRRPGCorePlugin, player: Player) {
            val view = JewelCombineView(rpg)
            openViews[player.uniqueId] = view
            view.open(player)
        }

        fun isOpen(uuid: UUID) = openViews.containsKey(uuid)
        fun getView(uuid: UUID): JewelCombineView? = openViews[uuid]

        fun closeAll() {
            val uuids = ArrayList(openViews.keys)
            openViews.clear()
            uuids.forEach { uuid ->
                CRRPGCorePlugin.plugin.server.getPlayer(uuid)?.closeInventory()
            }
        }
    }

    override fun io.zlero.cRFramework.view.scope.CreateScope.onCreate() {
        fill(rows = 3) { item(makeItem(Material.GRAY_STAINED_GLASS_PANE, "§r")) }

        // 재료 슬롯 (0~5)
        for (i in 0..5) {
            val idx = i
            button(slot = idx) {
                item { _ ->
                    materialSlots[idx] ?: emptyMaterial(idx + 1)
                }
                onClick { player ->
                    val mat = materialSlots[idx] ?: return@onClick
                    if (resultItem != null) {
                        player.sendMessage("§c[!] 결과 아이템을 먼저 수령하세요.")
                        return@onClick
                    }
                    materialSlots[idx] = null
                    player.inventory.addItem(mat)
                    rerender()
                }
            }
        }

        // 결과 미리보기 슬롯
        button(slot = SLOT_RESULT) {
            item { _ ->
                resultItem ?: previewResult()
            }
            onClick { player ->
                val res = resultItem ?: return@onClick
                resultItem = null
                player.inventory.addItem(res)
                player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 1f, 1f)
                rerender()
            }
        }

        // 합성 버튼
        button(slot = SLOT_COMBINE) {
            item { _ -> combineButton() }
            onClick { player ->
                if (resultItem != null) {
                    player.sendMessage("§c[!] 결과 아이템을 먼저 수령하세요.")
                    return@onClick
                }
                performCombine(player)
            }
        }
    }

    override fun onClose(player: Player) {
        openViews.remove(player.uniqueId)
        materialSlots.filterNotNull().forEach { player.inventory.addItem(it) }
        resultItem?.let { player.inventory.addItem(it) }
    }

    private fun performCombine(player: Player) {
        val filled = materialSlots.filterNotNull()
        if (filled.size < 3) {
            player.sendMessage("§c[!] 보석이 3개 이상 필요합니다.")
            return
        }
        val mats = filled.take(3)
        val jm = rpg.jewelManager

        if (mats.any { !jm.isAppraised(it) }) {
            player.sendMessage("§c[!] 모든 재료 보석이 감정된 상태여야 합니다.")
            return
        }

        val grades = mats.mapNotNull {
            it.itemMeta?.persistentDataContainer?.get(jm.keyJewelGrade, org.bukkit.persistence.PersistentDataType.STRING)
                ?.let { id -> JewelGrade.fromId(id) }
        }
        if (grades.size != 3 || grades.toSet().size != 1) {
            player.sendMessage("§c[!] 재료 보석이 모두 같은 등급이어야 합니다.")
            return
        }
        val srcGrade = grades[0]
        val dstGrade = when (srcGrade) {
            JewelGrade.LOW    -> JewelGrade.MID
            JewelGrade.MID    -> JewelGrade.HIGH
            JewelGrade.HIGH   -> JewelGrade.SUPREME
            JewelGrade.SUPREME -> { player.sendMessage("§c[!] 최상급 보석은 더 합성할 수 없습니다."); return }
        }

        val usedIndices = materialSlots.indices.filter { materialSlots[it] != null }.take(3)
        usedIndices.forEach { materialSlots[it] = null }

        resultItem = jm.createJewel(dstGrade, 1)
        player.playSound(player.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.5f)
        player.sendMessage("§5[보석 합성] §f${srcGrade.displayName} 보석 3개 → §5${dstGrade.displayName} 보석§f 합성 완료!")
        rerender()
    }

    private fun previewResult(): ItemStack {
        val filled = materialSlots.filterNotNull()
        if (filled.size < 3) return makeItem(Material.GRAY_STAINED_GLASS_PANE, "§8합성 결과",
            listOf("§7보석 3개를 배치하세요"))
        val jm = rpg.jewelManager
        val mats = filled.take(3)
        val allAppraised = mats.all { jm.isAppraised(it) }
        val grades = mats.mapNotNull {
            it.itemMeta?.persistentDataContainer?.get(jm.keyJewelGrade, org.bukkit.persistence.PersistentDataType.STRING)
                ?.let { id -> JewelGrade.fromId(id) }
        }
        if (!allAppraised) return makeItem(Material.BARRIER, "§c미감정 보석 포함",
            listOf("§7모든 재료를 감정하세요"))
        if (grades.size != 3 || grades.toSet().size != 1) return makeItem(Material.BARRIER, "§c등급 불일치",
            listOf("§7같은 등급의 보석 3개가 필요합니다"))
        val src = grades[0]
        val dst = when (src) {
            JewelGrade.LOW    -> JewelGrade.MID
            JewelGrade.MID    -> JewelGrade.HIGH
            JewelGrade.HIGH   -> JewelGrade.SUPREME
            JewelGrade.SUPREME -> return makeItem(Material.BARRIER, "§c최대 등급", listOf("§7최상급은 합성 불가"))
        }
        return makeItem(Material.AMETHYST_SHARD, "${dst.color}✦ ${dst.displayName} 보석 (미감정)",
            listOf("§r", "  §7${src.displayName} 보석 3개 → §f${dst.displayName} 보석 1개",
                "§r", "  §e합성 버튼을 눌러 진행하세요"))
    }

    private fun combineButton(): ItemStack {
        val filled = materialSlots.filterNotNull()
        return if (filled.size >= 3)
            makeItem(Material.EMERALD, "§a▶ §f합성 실행",
                listOf("§r", "  §7재료 보석 3개를 소모합니다", "  §e클릭하여 합성"))
        else
            makeItem(Material.GRAY_STAINED_GLASS_PANE, "§8합성 버튼",
                listOf("§7보석 슬롯에 재료를 배치하세요"))
    }

    private fun emptyMaterial(num: Int) = makeItem(Material.PURPLE_STAINED_GLASS_PANE,
        "§8재료 슬롯 $num", listOf("§7보석을 배치하세요"))

    private fun makeItem(mat: Material, name: String, lore: List<String> = emptyList()): ItemStack {
        val s = ItemStack(mat)
        val m: ItemMeta = s.itemMeta
        m.setDisplayName(name)
        if (lore.isNotEmpty()) m.lore = lore
        s.itemMeta = m
        return s
    }
}
