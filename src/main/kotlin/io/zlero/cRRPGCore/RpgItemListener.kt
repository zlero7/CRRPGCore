package io.zlero.cRRPGCore

import org.bukkit.entity.Player
import io.zlero.cRFramework.listener.annotation.Subscribe
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import kotlin.random.Random

/**
 * RPG 무기/장비/보석 전투 효과 리스너
 *
 * 데미지 계산 순서:
 *   ① 바닐라 데미지를 0으로 초기화 (RPG 무기인 경우)
 *   ② 무기 기본 데미지(keyWeaponDamage) + 강화 보너스(getDamageBonus) + 감정 추가 데미지(keyDamage) + 보석 보너스
 *   ③ 치명타 적용
 *   ④ 힘 스텟 보너스는 StatListener(HIGHEST)에서 추가
 */
class RpgItemListener(private val plugin: CRRPGCorePlugin) {

    // onAttack: HIGH → 데미지 설정 또는 취소
    @Subscribe(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onAttack(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val weapon   = attacker.inventory.itemInMainHand
        val wStat    = plugin.rpgItemManager.getWeaponStat(weapon) ?: return

        // RPG 무기인데 데미지 미설정 시 완전 차단
        val pdc = weapon.itemMeta?.persistentDataContainer
        val baseDmg = pdc?.get(
            plugin.rpgItemManager.keyWeaponDamage,
            org.bukkit.persistence.PersistentDataType.INTEGER
        )
        if (baseDmg == null) {
            event.isCancelled = true
            attacker.sendMessage(plugin.msgCfg.errWeaponNoDamage)
            return
        }

        val upgLv    = plugin.upgradeManager.getLevel(weapon)
        val upgBonus = plugin.upgradeManager.getDamageBonus(upgLv)

        val jewelStats  = plugin.jewelManager.getTotalStats(attacker)
        val jewelDmg    = (jewelStats[JewelStatType.DAMAGE]      ?: 0.0).toInt()
        val jewelCritCh = jewelStats[JewelStatType.CRIT_CHANCE]  ?: 0.0
        val jewelCritDm = jewelStats[JewelStatType.CRIT_DAMAGE]  ?: 0.0

        var damage = (baseDmg + upgBonus + wStat.damage + jewelDmg).toDouble()

        val totalCritCh = wStat.critChance + jewelCritCh
        val totalCritDm = wStat.critDamage + jewelCritDm
        val isCrit = Random.nextDouble() * 100.0 < totalCritCh
        if (isCrit) {
            damage *= (1.0 + totalCritDm / 100.0)
            attacker.sendMessage(plugin.msgCfg.msgCombatCrit)
        }

        event.damage = damage

        val mc = plugin.msgCfg
        if (mc.showCombatOut) {
            val targetName = (event.entity as? Player)?.name ?: event.entity.type.name
            val critTag    = if (isCrit) " §e[치명타]" else ""
            attacker.sendMessage(mc.format(mc.msgCombatOut,
                "target"  to targetName,
                "damage"  to String.format("%.1f", damage),
                "crit"    to critTag,
                "base"    to baseDmg.toString(),
                "upg"     to upgBonus.toString(),
                "appr"    to wStat.damage.toString(),
                "jewel"   to jewelDmg.toString()
            ))
        }
    }

    // onDefend: HIGHEST → onAttack(HIGH) + StatListener(HIGHEST) 이후 방어 계산
    @Subscribe(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDefend(event: EntityDamageByEntityEvent) {
        val victim     = event.entity as? Player ?: return
        val armorStat  = plugin.rpgItemManager.getTotalArmorStat(victim)
        val jewelStats = plugin.jewelManager.getTotalStats(victim)

        val totalEvasion = armorStat.evasion + (jewelStats[JewelStatType.EVASION] ?: 0.0)
        if (totalEvasion > 0.0 && Random.nextDouble() * 100.0 < totalEvasion) {
            event.isCancelled = true
            val mc = plugin.msgCfg
            victim.sendMessage(mc.msgArmorEvasion)
            (event.damager as? Player)?.sendMessage(mc.format(mc.msgArmorEvasionFoe, "target" to victim.name))
            return
        }

        val attacker = event.damager as? Player
        val penPct = if (attacker != null) {
            val wPen = plugin.rpgItemManager.getWeaponStat(attacker.inventory.itemInMainHand)?.penetration ?: 0.0
            val jPen = plugin.jewelManager.getTotalStats(attacker)[JewelStatType.PENETRATION] ?: 0.0
            wPen + jPen
        } else 0.0

        val totalDef = (armorStat.defense + (jewelStats[JewelStatType.DEFENSE] ?: 0.0))
            .coerceAtMost(plugin.rpgItemManager.maxDefense)
        val effectiveDef = (totalDef * (1.0 - penPct / 100.0)).coerceAtLeast(0.0)
        if (effectiveDef > 0.0) event.damage *= (1.0 - effectiveDef / 100.0)

        if (attacker != null) {
            val wStat   = plugin.rpgItemManager.getWeaponStat(attacker.inventory.itemInMainHand)
            val jewelLs = plugin.jewelManager.getTotalStats(attacker)[JewelStatType.LIFE_STEAL] ?: 0.0
            val totalLs = (wStat?.lifeSteal ?: 0.0) + jewelLs
            val actualDmg = event.damage

            if (totalLs > 0.0) {
                val healAmount = actualDmg * (totalLs / 100.0)
                val maxHp = attacker.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)?.value ?: 20.0
                attacker.health = (attacker.health + healAmount).coerceAtMost(maxHp)
                attacker.sendMessage(plugin.msgCfg.format(plugin.msgCfg.msgLifeSteal,
                    "amount" to String.format("%.1f", healAmount)))
            }
        }

        // 방어구 스탯 캐시 무효화 (피격자 방어구 변경 가능성)
        plugin.rpgItemManager.invalidateArmorCache(victim.uniqueId)

        val mc = plugin.msgCfg
        if (mc.showCombatIn) {
            val attackerName = (event.damager as? Player)?.name ?: event.damager.type.name
            victim.sendMessage(mc.format(mc.msgCombatIn,
                "attacker" to attackerName,
                "damage"   to String.format("%.1f", event.damage),
                "defense"  to String.format("%.1f", effectiveDef),
                "pen"      to String.format("%.1f", penPct)
            ))
        }
    }

    // 장비 슬롯 변경 시 방어구 스탯 캐시 무효화 (#6)
    @Subscribe(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val slot = event.rawSlot
        // 방어구 슬롯: 5(헬멧), 6(흉갑), 7(레깅스), 8(부츠)
        if (slot in 5..8 || event.slot in 36..39) {
            plugin.rpgItemManager.invalidateArmorCache(player.uniqueId)
        }
    }

    @Subscribe(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onItemHeld(event: PlayerItemHeldEvent) {
        // 손에 든 무기 변경 시에는 보석 캐시도 무효화
        plugin.jewelManager.invalidateStatsCache(event.player.uniqueId)
    }

    @Subscribe(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onItemDrop(event: org.bukkit.event.player.PlayerDropItemEvent) {
        val player = event.player
        val item   = event.itemDrop.itemStack
        if (!plugin.rpgItemManager.isBound(item)) return
        val owner = plugin.rpgItemManager.getBoundOwner(item) ?: return
        if (owner != player.uniqueId) {
            event.isCancelled = true
            player.sendMessage("§c[!] §c귀속된 아이템은 버릴 수 없습니다.")
        }
    }

    @Subscribe(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPickup(event: org.bukkit.event.entity.EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        val item   = event.item.itemStack
        if (!plugin.rpgItemManager.isBound(item)) return
        val owner = plugin.rpgItemManager.getBoundOwner(item) ?: return
        if (owner != player.uniqueId) {
            event.isCancelled = true
            player.sendMessage("§c[!] §c다른 플레이어에게 귀속된 아이템입니다.")
        }
    }
}