package io.zlero.cRRPGCore

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class RpgCoreCommand(private val plugin: CRRPGCorePlugin) : CommandExecutor, TabCompleter {

    private val gradeIds = ItemGrade.entries.map { it.id }

    override fun onCommand(
        sender: CommandSender, command: Command,
        label: String, args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) { sendHelp(sender); return true }

        when (args[0].lowercase()) {
            "level"   -> handleLevel(sender, args.drop(1))
            "stat"    -> handleStat(sender, args.drop(1))
            "weapon"  -> handleItem(sender, args.drop(1), isWeapon = true)
            "armor"   -> handleItem(sender, args.drop(1), isWeapon = false)
            "upgrade" -> handleUpgrade(sender, args.drop(1))
            "roon"    -> handleRoon(sender, args.drop(1))
            "jewelry" -> handleJewelry(sender, args.drop(1))
            "info"    -> handleInfo(sender, args.drop(1))
            "awake"   -> handleAwake(sender, args.drop(1))
            "reload"  -> handleReload(sender)
            "migrate" -> handleMigrate(sender)
            else      -> sendHelp(sender)
        }
        return true
    }

    // ─────────────────────────────────────────────────────────────────
    //  migrate  (YAML → 현재 DB)
    // ─────────────────────────────────────────────────────────────────
    private fun handleMigrate(sender: CommandSender) {
        if (!sender.hasPermission("crrpgcore.admin")) {
            sender.sendMessage(plugin.msgCfg.errNoPermission); return
        }
        sender.sendMessage("§e[CRRPGCore] §fPlayerData.yml → DB 마이그레이션을 시작합니다...")
        // 비동기로 실행 (DB I/O 부하를 메인 스레드에서 분리)
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val result = plugin.migrationManager.migrateFromYaml()
            plugin.server.scheduler.runTask(plugin, Runnable {
                sender.sendMessage("§a[CRRPGCore] §f마이그레이션 완료!")
                sender.sendMessage("  §7플레이어 §8: §a${result.playerMigrated}명 이전 §8/ §e${result.playerSkipped}명 스킵")
                sender.sendMessage("  §7룬 슬롯  §8: §a${result.roonMigrated}명 이전")
                if (result.hasErrors)
                    sender.sendMessage("  §c오류 §8: §c${result.errors}건 §7(서버 콘솔 로그 확인)")
            })
        })
    }

    // ─────────────────────────────────────────────────────────────────
    //  reload
    // ─────────────────────────────────────────────────────────────────
    private fun handleReload(sender: CommandSender) {
        val mc = plugin.msgCfg
        if (!sender.hasPermission("crrpgcore.admin")) { sender.sendMessage(mc.errNoPermission); return }
        plugin.reloadAll()
        sender.sendMessage(plugin.msgCfg.msgAdminReload)
    }

    // ─────────────────────────────────────────────────────────────────
    //  level
    // ─────────────────────────────────────────────────────────────────
    private fun handleLevel(sender: CommandSender, args: List<String>) {
        val mc = plugin.msgCfg
        if (args.isEmpty()) { sendLevelHelp(sender); return }

        when (args[0].lowercase()) {

            "info" -> {
                val target: Player = if (args.size >= 2) {
                    if (!sender.hasPermission("crrpgcore.admin")) { sender.sendMessage(mc.errNoPermission); return }
                    Bukkit.getPlayer(args[1]) ?: run { sender.sendMessage(mc.format(mc.errPlayerNotFound, "player" to args[1])); return }
                } else {
                    (sender as? Player) ?: run { sender.sendMessage(mc.errConsoleNoPlayer); return }
                }
                val lm       = plugin.levelManager
                val data     = lm.getPlayerData(target)
                val required = lm.getRequiredXpForLevel(data.level)
                val isMax    = data.level >= lm.maxLevel
                val reqStr   = if (isMax) "MAX" else required.toString()
                val remaining = if (isMax) 0L else (required - data.xp).coerceAtLeast(0L)
                sender.sendMessage("§8────────────────────────────")
                sender.sendMessage("§e${target.name}§f의 레벨 정보")
                sender.sendMessage("§7레벨: §b${data.level} §8/ §7최대: §b${lm.maxLevel}")
                sender.sendMessage("§7경험치: §a${data.xp} §8/ §a$reqStr")
                sender.sendMessage("§7다음 레벨까지: §d$remaining XP")
                sender.sendMessage("§8────────────────────────────")
            }

            "setlevel" -> {
                if (!sender.hasPermission("crrpgcore.admin")) { sender.sendMessage(mc.errNoPermission); return }
                if (args.size < 3) { sender.sendMessage("§c[!] §c사용법: /rpgcore level setlevel <player> <level>"); return }
                val target = Bukkit.getPlayer(args[1]) ?: run { sender.sendMessage(mc.format(mc.errPlayerNotFound, "player" to args[1])); return }
                val level  = args[2].toIntOrNull() ?: run { sender.sendMessage("§c[!] §c레벨은 숫자여야 합니다."); return }
                plugin.levelManager.setLevel(target, level)
                sender.sendMessage(mc.format(mc.msgAdminLevelSet, "player" to target.name, "level" to level.toString()))
                target.sendMessage(mc.format(mc.msgAdminLevelSetPlayer, "level" to level.toString()))
            }

            "setxp" -> {
                if (!sender.hasPermission("crrpgcore.admin")) { sender.sendMessage(mc.errNoPermission); return }
                if (args.size < 3) { sender.sendMessage("§c[!] §c사용법: /rpgcore level setxp <player> <xp>"); return }
                val target = Bukkit.getPlayer(args[1]) ?: run { sender.sendMessage(mc.format(mc.errPlayerNotFound, "player" to args[1])); return }
                val xp     = args[2].toLongOrNull() ?: run { sender.sendMessage("§c[!] §cXP는 숫자여야 합니다."); return }
                plugin.levelManager.setXp(target, xp)
                sender.sendMessage(mc.format(mc.msgAdminXpSet, "player" to target.name, "xp" to xp.toString()))
            }

            "givexp" -> {
                if (!sender.hasPermission("crrpgcore.admin")) { sender.sendMessage(mc.errNoPermission); return }
                if (args.size < 3) { sender.sendMessage("§c[!] §c사용법: /rpgcore level givexp <player> <xp>"); return }
                val target = Bukkit.getPlayer(args[1]) ?: run { sender.sendMessage(mc.format(mc.errPlayerNotFound, "player" to args[1])); return }
                val xp     = args[2].toLongOrNull() ?: run { sender.sendMessage("§c[!] §cXP는 숫자여야 합니다."); return }
                plugin.levelManager.giveXp(target, xp)
                sender.sendMessage(mc.format(mc.msgAdminXpGive, "player" to target.name, "xp" to xp.toString()))
            }

            "reload" -> {
                if (!sender.hasPermission("crrpgcore.admin")) { sender.sendMessage(mc.errNoPermission); return }
                plugin.reloadAll()
                sender.sendMessage(mc.msgAdminReload)
            }

            "초기화권" -> {
                if (!sender.hasPermission("crrpgcore.admin")) { sender.sendMessage(mc.errNoPermission); return }
                if (args.size < 2) { sender.sendMessage("§c사용법: /rpgcore level 초기화권 <player> [갯수]"); return }
                val target = Bukkit.getPlayer(args[1]) ?: run { sender.sendMessage(mc.format(mc.errPlayerNotFound, "player" to args[1])); return }
                val amount = if (args.size >= 3) args[2].toIntOrNull() ?: 1 else 1
                val scroll = LevelResetScroll.createItem(amount)
                target.inventory.addItem(scroll)
                sender.sendMessage(mc.format(mc.msgAdminGiveScroll,
                    "player" to target.name,
                    "item"   to mc.levelResetScrollName,
                    "count"  to amount.toString()))
                target.sendMessage(mc.format(mc.msgReceiveScroll,
                    "item"  to mc.levelResetScrollName,
                    "count" to amount.toString()))
            }

            "xpboost" -> {
                if (!sender.hasPermission("crrpgcore.admin")) { sender.sendMessage(mc.errNoPermission); return }
                if (args.size < 4) {
                    sender.sendMessage("§c사용법: /rpgcore level xpboost <배수> <시간(분)> <개인/전체>")
                    sender.sendMessage("§7예: §e/rpgcore level xpboost 2.0 30 개인")
                    return
                }
                val multiplier = args[1].toDoubleOrNull()
                if (multiplier == null || multiplier <= 0) { sender.sendMessage("§c[!] 배수는 0보다 큰 숫자여야 합니다."); return }
                val minutes = args[2].toIntOrNull()
                if (minutes == null || minutes <= 0) { sender.sendMessage("§c[!] 시간은 0보다 큰 정수(분)여야 합니다."); return }
                val scope = when (args[3]) {
                    "전체", "global"   -> "global"
                    "개인", "personal" -> "personal"
                    else -> { sender.sendMessage("§c[!] 범위는 개인 또는 전체여야 합니다."); return }
                }
                val amount = if (args.size >= 5) args[4].toIntOrNull() ?: 1 else 1
                val target = if (args.size >= 6) {
                    Bukkit.getPlayer(args[5]) ?: run { sender.sendMessage(mc.format(mc.errPlayerNotFound, "player" to args[5])); return }
                } else {
                    (sender as? Player) ?: run { sender.sendMessage(mc.errConsoleNoPlayer); return }
                }

                val scroll = XpBoostScroll.createScroll(multiplier, minutes, scope, amount)
                target.inventory.addItem(scroll)
                val scopeLabel = if (scope == "global") "전체" else "개인"
                sender.sendMessage("§a[!] ${target.name}에게 §6경험치 부스트권 §8(§e${multiplier}x §7/ §b${minutes}분 §7/ $scopeLabel§8) §f${amount}개§a를 지급했습니다.")
                if (target != sender) target.sendMessage("§e[!] §6경험치 부스트권 §f${amount}개§e를 받았습니다! §7(우클릭하여 사용)")
            }

            else -> sendLevelHelp(sender)
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  stat
    // ─────────────────────────────────────────────────────────────────
    private fun handleStat(sender: CommandSender, args: List<String>) {
        val mc = plugin.msgCfg
        if (args.isEmpty()) {
            val player = sender as? Player ?: run { sender.sendMessage(mc.errConsoleNoPlayer); return }
            StatView(plugin).open(player)
            return
        }

        when (args[0].lowercase()) {

            "info" -> {
                val target: Player = if (args.size >= 2) {
                    if (!sender.hasPermission("crrpgcore.admin")) { sender.sendMessage(mc.errNoPermission); return }
                    Bukkit.getPlayer(args[1]) ?: run { sender.sendMessage(mc.format(mc.errPlayerNotFound, "player" to args[1])); return }
                } else {
                    (sender as? Player) ?: run { sender.sendMessage(mc.errConsoleNoPlayer); return }
                }
                sender.sendMessage("§8────────────────────────────")
                sender.sendMessage("§e${target.name}§f의 스텟 정보")
                plugin.statManager.getSummary(target).forEach { sender.sendMessage(it) }
                sender.sendMessage("§8────────────────────────────")
            }

            "reset" -> {
                if (!sender.hasPermission("crrpgcore.admin")) { sender.sendMessage(mc.errNoPermission); return }
                if (args.size < 2) { sender.sendMessage("§c[!] §c사용법: /rpgcore stat reset <player>"); return }
                val target = Bukkit.getPlayer(args[1]) ?: run { sender.sendMessage(mc.format(mc.errPlayerNotFound, "player" to args[1])); return }
                val data     = plugin.levelManager.getPlayerData(target)
                val totalPts = data.strength + data.vitality + data.agility
                plugin.playerDataRepository.update(target.uniqueId) {
                    statPoints += totalPts
                    strength = 0; vitality = 0; agility = 0
                }
                plugin.statManager.applyVitality(target)
                plugin.playerDataRepository.flush(target.uniqueId)
                val newData = plugin.levelManager.getPlayerData(target)
                sender.sendMessage(mc.format(mc.msgAdminStatReset, "player" to target.name, "points" to newData.statPoints.toString()))
                target.sendMessage(mc.format(mc.msgAdminStatResetPlayer, "points" to newData.statPoints.toString()))
            }

            "초기화권" -> {
                if (!sender.hasPermission("crrpgcore.admin")) { sender.sendMessage(mc.errNoPermission); return }
                if (args.size < 2) { sender.sendMessage("§c사용법: /rpgcore stat 초기화권 <player> [갯수]"); return }
                val target = Bukkit.getPlayer(args[1]) ?: run { sender.sendMessage(mc.format(mc.errPlayerNotFound, "player" to args[1])); return }
                val amount = if (args.size >= 3) args[2].toIntOrNull() ?: 1 else 1
                val scroll = StatResetScroll.createItem(amount)
                target.inventory.addItem(scroll)
                sender.sendMessage(mc.format(mc.msgAdminGiveScroll,
                    "player" to target.name,
                    "item"   to mc.statResetScrollName,
                    "count"  to amount.toString()))
                target.sendMessage(mc.format(mc.msgReceiveScroll,
                    "item"  to mc.statResetScrollName,
                    "count" to amount.toString()))
            }

            else -> sendStatHelp(sender)
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  weapon / armor
    // ─────────────────────────────────────────────────────────────────
    private fun handleItem(sender: CommandSender, args: List<String>, isWeapon: Boolean) {
        val mc = plugin.msgCfg
        if (!sender.hasPermission("crrpgcore.admin")) { sender.sendMessage(mc.errNoPermission); return }
        val player = sender as? Player ?: run { sender.sendMessage(mc.errConsoleUnavail); return }
        if (args.isEmpty()) { sendItemHelp(sender, isWeapon); return }

        when (args[0].lowercase()) {

            "rank" -> {
                if (args.size < 2) {
                    sender.sendMessage("§c[!] 등급을 입력하세요.")
                    sender.sendMessage("§7등급 목록: §f${gradeIds.joinToString(", ")}")
                    return
                }
                val grade = ItemGrade.fromId(args[1]) ?: ItemGrade.fromKorean(args[1]) ?: run {
                    sender.sendMessage("§c[!] 알 수 없는 등급: §e${args[1]}")
                    sender.sendMessage("§7등급 목록: §f${gradeIds.joinToString(", ")}")
                    return
                }
                val item = player.inventory.itemInMainHand
                if (item.type.isAir) { sender.sendMessage(mc.errNeedItemInHand); return }

                val success = plugin.rpgItemManager.setGrade(item, grade, isWeapon)
                if (success) {
                    val typeLabel = if (isWeapon) "무기" else "장비"
                    sender.sendMessage(mc.format(mc.msgAdminWeaponRank,
                        "material" to item.type.name,
                        "grade"    to "${grade.color}${grade.displayName} $typeLabel§a"))
                    sender.sendMessage(mc.msgAdminWeaponRankTip)
                } else {
                    sender.sendMessage("§c[!] 등급 설정에 실패했습니다.")
                }
            }

            "damage" -> {
                if (!isWeapon) { sender.sendMessage("§c[!] 무기에만 사용할 수 있는 명령어입니다."); return }
                if (args.size < 2) { sender.sendMessage("§c[!] 사용법: /rpgcore weapon damage <값>"); return }
                val dmg = args[1].toIntOrNull()
                if (dmg == null || dmg < 0) { sender.sendMessage("§c[!] 데미지 값은 0 이상의 정수여야 합니다."); return }

                val item = player.inventory.itemInMainHand
                if (item.type.isAir) { sender.sendMessage(mc.errNeedWeaponInHand); return }

                val success = plugin.rpgItemManager.setWeaponDamage(item, dmg)
                if (success) {
                    val upgLv   = plugin.upgradeManager.getLevel(item)
                    val bonus   = plugin.upgradeManager.getDamageBonus(upgLv)
                    val dispDmg = dmg + bonus
                    val upgNote = if (upgLv > 0) " §8(현재 §6+$upgLv §8강화, 실제 표시: §c$dispDmg§8)" else ""
                    sender.sendMessage(mc.format(mc.msgAdminWeaponDamage,
                        "damage"  to dmg.toString(),
                        "upgNote" to upgNote))
                } else {
                    sender.sendMessage("§c[!] RPG 무기 타입의 아이템이 아닙니다. 먼저 §e/rpgcore weapon rank§c 로 등급을 설정하세요.")
                }
            }

            else -> sendItemHelp(sender, isWeapon)
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  upgrade
    // ─────────────────────────────────────────────────────────────────
    private fun handleUpgrade(sender: CommandSender, args: List<String>) {
        val mc     = plugin.msgCfg
        val player = sender as? Player ?: run { sender.sendMessage(mc.errConsoleUnavail); return }

        if (args.isEmpty()) { UpgradeGui.open(player); return }

        if (!sender.hasPermission("crrpgcore.admin")) { sender.sendMessage(mc.errNoPermission); return }

        when (args[0].lowercase()) {
            "파괴방어", "파괴방지" -> {
                val amount = if (args.size >= 2) args[1].toIntOrNull() ?: 1 else 1
                val target = if (args.size >= 3) Bukkit.getPlayer(args[2]) ?: run { sender.sendMessage(mc.format(mc.errPlayerNotFound, "player" to args[2])); return } else player
                val scroll = plugin.upgradeManager.createProtectScroll("break", amount)
                target.inventory.addItem(scroll)
                sender.sendMessage(mc.format(mc.msgAdminGiveScroll,
                    "player" to target.name, "item" to mc.protectBreakName, "count" to amount.toString()))
                if (target != player) target.sendMessage(mc.format(mc.msgReceiveScroll,
                    "item" to mc.protectBreakName, "count" to amount.toString()))
            }
            "하락방어", "하락방지" -> {
                val amount = if (args.size >= 2) args[1].toIntOrNull() ?: 1 else 1
                val target = if (args.size >= 3) Bukkit.getPlayer(args[2]) ?: run { sender.sendMessage(mc.format(mc.errPlayerNotFound, "player" to args[2])); return } else player
                val scroll = plugin.upgradeManager.createProtectScroll("down", amount)
                target.inventory.addItem(scroll)
                sender.sendMessage(mc.format(mc.msgAdminGiveScroll,
                    "player" to target.name, "item" to mc.protectDownName, "count" to amount.toString()))
                if (target != player) target.sendMessage(mc.format(mc.msgReceiveScroll,
                    "item" to mc.protectDownName, "count" to amount.toString()))
            }
            "강화석" -> {
                if (args.size < 2) { sender.sendMessage("§c사용법: /rpgcore upgrade 강화석 <low/mid/high> [갯수] [플레이어]"); return }
                val stoneType = args[1].lowercase()
                if (stoneType !in listOf("low", "mid", "high")) { sender.sendMessage("§c종류: low(하급), mid(중급), high(상급)"); return }
                val amount = if (args.size >= 3) args[2].toIntOrNull() ?: 1 else 1
                val target = if (args.size >= 4) Bukkit.getPlayer(args[3]) ?: run { sender.sendMessage(mc.format(mc.errPlayerNotFound, "player" to args[3])); return } else player
                val stone  = plugin.upgradeManager.createStone(stoneType, amount)
                target.inventory.addItem(stone)
                val gradeName = when(stoneType) { "low" -> "하급"; "mid" -> "중급"; else -> "상급" }
                sender.sendMessage(mc.format(mc.msgAdminGiveStone,
                    "player" to target.name, "grade" to gradeName, "count" to amount.toString()))
                if (target != player) target.sendMessage(mc.format(mc.msgReceiveStone,
                    "grade" to gradeName, "count" to amount.toString()))
            }
            else -> {
                sender.sendMessage("§7사용법:")
                sender.sendMessage("§e/rpgcore upgrade §7- 강화 GUI 오픈")
                sender.sendMessage("§e/rpgcore upgrade 파괴방어 §8[갯수] [플레이어]")
                sender.sendMessage("§e/rpgcore upgrade 하락방어 §8[갯수] [플레이어]")
                sender.sendMessage("§e/rpgcore upgrade 강화석 §8<low/mid/high> [갯수] [플레이어]")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  info
    // ─────────────────────────────────────────────────────────────────
    private fun handleInfo(sender: CommandSender, args: List<String>) {
        val mc = plugin.msgCfg
        val target: Player = if (args.isNotEmpty()) {
            if (!sender.hasPermission("crrpgcore.admin")) { sender.sendMessage(mc.errNoPermission); return }
            Bukkit.getPlayer(args[0]) ?: run { sender.sendMessage(mc.format(mc.errPlayerNotFound, "player" to args[0])); return }
        } else {
            (sender as? Player) ?: run { sender.sendMessage(mc.errConsoleNoPlayer); return }
        }

        val data      = plugin.levelManager.getPlayerData(target)
        val statMgr   = plugin.statManager
        val attr      = target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)
        val maxHp     = attr?.value ?: 20.0
        val armorStat = plugin.rpgItemManager.getTotalArmorStat(target)
        val jewelMap  = plugin.jewelManager.getTotalStats(target)
        val weapon    = target.inventory.itemInMainHand
        val wStat     = plugin.rpgItemManager.getWeaponStat(weapon)
        val upgLv     = plugin.upgradeManager.getLevel(weapon)
        val upgBonus  = plugin.upgradeManager.getDamageBonus(upgLv)
        val wpnBase   = plugin.rpgItemManager.getWeaponBaseDamage(weapon)

        val sep = "§8────────────────────────────────"
        sender.sendMessage(sep)
        sender.sendMessage("  §e✦ §f${target.name} §7정보  §8[레벨 ${data.level}]")
        sender.sendMessage(sep)
        val armorHp = armorStat.health.toDouble()
        val jewelHp = jewelMap[JewelStatType.HEALTH] ?: 0.0
        sender.sendMessage("  §a♥ 생명력")
        sender.sendMessage("    §7현재 HP §8: §f${String.format("%.1f", target.health)} §8/ §a${String.format("%.1f", maxHp)}")
        sender.sendMessage("    §7기본 §8: §f20  §7체력스탯 §8: §a+${(data.vitality * 2).toInt()}  §7장비 §8: §a+${armorHp.toInt()}  §7보석 §8: §a+${jewelHp.toInt()}")
        sender.sendMessage("")
        sender.sendMessage("  §e★ 스탯 포인트")
        sender.sendMessage("    §7잔여 §8: §e${data.statPoints}P")
        sender.sendMessage("    §c힘   §8[§f${data.strength}§8/§7${statMgr.maxStrength}§8]  §7데미지 +${(data.strength * StatType.STRENGTH.effectPerPoint).toInt()}")
        sender.sendMessage("    §a체력 §8[§f${data.vitality}§8/§7${statMgr.maxVitality}§8]  §7HP +${(data.vitality * StatType.VITALITY.effectPerPoint).toInt()}")
        sender.sendMessage("    §b민체 §8[§f${data.agility}§8/§7${statMgr.maxAgility}§8]  §7회피 ${String.format("%.1f", data.agility * StatType.AGILITY.effectPerPoint)}%")
        sender.sendMessage("")
        sender.sendMessage("  §7■ 장비 합산 스탯")
        sender.sendMessage("    §7방어력 §8: §b${String.format("%.1f", armorStat.defense)}%  §7회피율 §8: §e${String.format("%.1f", armorStat.evasion)}%")
        if (jewelMap.isNotEmpty()) {
            sender.sendMessage("")
            sender.sendMessage("  §6✦ 보석(루) 합산 스탯")
            val equipped = plugin.jewelManager.getSlots(target).filterNotNull().size
            sender.sendMessage("    §7장착 수 §8: §f$equipped / 9")
            jewelMap.entries.sortedBy { it.key.name }.forEach { (type, value) ->
                val label = when (type) {
                    JewelStatType.DAMAGE       -> "§c데미지"
                    JewelStatType.CRIT_CHANCE  -> "§e치명타확률"
                    JewelStatType.CRIT_DAMAGE  -> "§e치명타피해"
                    JewelStatType.ATTACK_SPEED -> "§a공격속도"
                    JewelStatType.PENETRATION  -> "§b관통"
                    JewelStatType.LIFE_STEAL   -> "§c흡혈"
                    JewelStatType.HEALTH       -> "§a생명력"
                    JewelStatType.DEFENSE      -> "§b방어력"
                    JewelStatType.EVASION      -> "§e회피율"
                }
                val fmt = if (type.isInteger) "+${value.toInt()}" else "+${String.format("%.1f", value)}%"
                sender.sendMessage("    $label §8: §f$fmt")
            }
        }
        if (wStat != null) {
            sender.sendMessage("")
            val upgNote = if (upgLv > 0) " §8(+$upgLv 강화, 보너스 §c+$upgBonus§8)" else ""
            sender.sendMessage("  §c⚔ 이운 무기 스탯$upgNote")
            if (wpnBase > 0) sender.sendMessage("    §7무기 기본 데미지 §8: §c$wpnBase${if (upgLv > 0) " §8→ §c${wpnBase + upgBonus}" else ""}")
            sender.sendMessage("    §7감정 추가 데미지 §8: §f+${wStat.damage}  §7치명타확률 §8: §e${String.format("%.1f", wStat.critChance)}%  §7치명타피해 §8: §e${String.format("%.0f", wStat.critDamage)}%")
            sender.sendMessage("    §7공격속도 §8: §a${String.format("%.1f", wStat.attackSpeed)}%  §7관통 §8: §b${String.format("%.1f", wStat.penetration)}%  §7흡혈 §8: §c${String.format("%.1f", wStat.lifeSteal)}%")
        }
        sender.sendMessage(sep)
    }

    // ─────────────────────────────────────────────────────────────────
    //  roon / jewelry / awake
    // ─────────────────────────────────────────────────────────────────
    private fun handleRoon(sender: CommandSender, args: List<String>) {
        val player = sender as? Player ?: run { sender.sendMessage(plugin.msgCfg.errConsoleUnavail); return }
        RoonGui.open(player)
    }

    private fun handleJewelry(sender: CommandSender, args: List<String>) {
        val mc = plugin.msgCfg
        if (!sender.hasPermission("crrpgcore.admin")) { sender.sendMessage(mc.errNoPermission); return }
        if (args.isEmpty()) { sender.sendMessage("§c사용법: /rpgcore jewelry <low/mid/high/supreme> [갯수] [플레이어]"); return }

        val grade = JewelGrade.fromId(args[0].lowercase()) ?: run {
            sender.sendMessage("§c[!] 등급: low / mid / high / supreme"); return
        }
        val amount = if (args.size >= 2) args[1].toIntOrNull() ?: 1 else 1
        val target = if (args.size >= 3) {
            Bukkit.getPlayer(args[2]) ?: run { sender.sendMessage(mc.format(mc.errPlayerNotFound, "player" to args[2])); return }
        } else {
            (sender as? Player) ?: run { sender.sendMessage(mc.errConsoleNoPlayer); return }
        }

        val jewel = plugin.jewelManager.createJewel(grade, amount)
        target.inventory.addItem(jewel)
        sender.sendMessage(mc.format(mc.msgAdminGiveJewel,
            "player" to target.name,
            "grade"  to "${grade.color}${grade.displayName}",
            "count"  to amount.toString()))
        if (target != sender) target.sendMessage(mc.format(mc.msgReceiveJewel,
            "grade" to "${grade.color}${grade.displayName}",
            "count" to amount.toString()))
    }

    private fun handleAwake(sender: CommandSender, args: List<String>) {
        val mc = plugin.msgCfg
        if (args.isEmpty()) {
            val player = sender as? Player ?: run { sender.sendMessage(mc.errConsoleUnavail); return }
            AwakeGui.open(player)
            return
        }

        if (!sender.hasPermission("crrpgcore.admin")) { sender.sendMessage(mc.errNoPermission); return }

        val type = args[0].lowercase()
        if (type !in listOf("scroll", "stone")) {
            sender.sendMessage("§c[!] 사용법: /rpgcore awake <scroll|stone> [플레이어] [갯수]"); return
        }

        val targetName = args.getOrNull(1)
        val amount     = args.getOrNull(2)?.toIntOrNull()?.coerceIn(1, 64) ?: 1
        val target     = if (targetName != null) {
            Bukkit.getPlayer(targetName) ?: run { sender.sendMessage(mc.format(mc.errPlayerNotFound, "player" to targetName)); return }
        } else {
            sender as? Player ?: run { sender.sendMessage(mc.errConsoleNoPlayer); return }
        }

        val scrollItem = AwakeGui.makeScrollItem(type) ?: run { sender.sendMessage("§c[!] 알 수 없는 타입: $type"); return }
        scrollItem.amount = amount
        target.inventory.addItem(scrollItem).values.forEach { leftover ->
            target.world.dropItem(target.location, leftover)
        }

        val typeName = if (type == "scroll") mc.scrollName else mc.stoneName
        sender.sendMessage(mc.format(mc.msgAdminGiveScroll,
            "player" to target.name, "item" to typeName, "count" to amount.toString()))
        if (target != sender) target.sendMessage(mc.format(mc.msgReceiveScroll,
            "item" to typeName, "count" to amount.toString()))
    }

    // ─────────────────────────────────────────────────────────────────
    //  도움말
    // ─────────────────────────────────────────────────────────────────
    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§8─── §bCRRPGCore 명령어 §8────────────────")
        sender.sendMessage("§e/rpgcore level  §8<subcommand>  §7레벨 관련")
        sender.sendMessage("§e/rpgcore stat   §8<subcommand>  §7스텟 관련")
        sender.sendMessage("§e/rpgcore weapon §8rank <등급>    §7무기 등급 지정 §7§o(OP)")
        sender.sendMessage("§e/rpgcore weapon §8damage <값>    §7무기 기본 데미지 설정 §7§o(OP)")
        sender.sendMessage("§e/rpgcore armor  §8rank <등급>    §7장비 등급 지정 §7§o(OP)")
        sender.sendMessage("§e/rpgcore upgrade               §7강화 GUI 열기")
        sender.sendMessage("§e/rpgcore upgrade §8파괴/하락방어   §7방지권 지급 §7§o(OP)")
        sender.sendMessage("§e/rpgcore info    §8[플레이어]      §7플레이어 종합 정보")
        sender.sendMessage("§e/rpgcore roon                  §7루 장착 GUI 열기")
        sender.sendMessage("§e/rpgcore jewelry §8<등급> [갯수] [플레이어]  §7보석 지급 §7§o(OP)")
        sender.sendMessage("§e/rpgcore awake                 §7각성/감정 GUI 열기")
        sender.sendMessage("§e/rpgcore reload                §7설정 리로드 §7§o(OP)")
        sender.sendMessage("§e/rpgcore migrate               §7YAML → DB 데이터 이전 §7§o(OP)")
        sender.sendMessage("§8────────────────────────────────")
    }

    private fun sendLevelHelp(sender: CommandSender) {
        sender.sendMessage("§8─── §b레벨 명령어 §8──────────────────────")
        sender.sendMessage("§e/rpgcore level info §8[player]")
        sender.sendMessage("§e/rpgcore level setlevel §8<player> <level>")
        sender.sendMessage("§e/rpgcore level setxp §8<player> <xp>")
        sender.sendMessage("§e/rpgcore level givexp §8<player> <xp>")
        sender.sendMessage("§e/rpgcore level reload")
        sender.sendMessage("§e/rpgcore level 초기화권 §8<player> [갯수]  §7§o(OP)")
        sender.sendMessage("§8────────────────────────────────")
    }

    private fun sendStatHelp(sender: CommandSender) {
        sender.sendMessage("§8─── §b스텟 명령어 §8──────────────────────")
        sender.sendMessage("§e/rpgcore stat §8(GUI 열기)")
        sender.sendMessage("§e/rpgcore stat info §8[player]")
        sender.sendMessage("§e/rpgcore stat reset §8<player>  §7§o(OP)")
        sender.sendMessage("§e/rpgcore stat 초기화권 §8<player> [갯수]  §7§o(OP)")
        sender.sendMessage("§8────────────────────────────────")
    }

    private fun sendItemHelp(sender: CommandSender, isWeapon: Boolean) {
        val type = if (isWeapon) "weapon" else "armor"
        sender.sendMessage("§8─── §b${if (isWeapon) "무기" else "장비"} 명령어 §8──────────────────────")
        sender.sendMessage("§e/rpgcore $type rank §8<등급>  §7등급 지정")
        if (isWeapon) sender.sendMessage("§e/rpgcore $type damage §8<값>   §7기본 데미지 설정")
        sender.sendMessage("§7등급 목록: §f${gradeIds.joinToString(", ")}")
        sender.sendMessage("§8────────────────────────────────")
    }

    // ─────────────────────────────────────────────────────────────────
    //  탭 완성
    // ─────────────────────────────────────────────────────────────────
    override fun onTabComplete(
        sender: CommandSender, command: Command,
        alias: String, args: Array<out String>
    ): List<String> {
        val online = Bukkit.getOnlinePlayers().map { it.name }
        return when (args.size) {
            1 -> listOf("level","stat","weapon","armor","upgrade","roon","jewelry","info","awake","reload","migrate")
                .filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> when (args[0].lowercase()) {
                "level"   -> listOf("info","setlevel","setxp","givexp","reload","초기화권","xpboost").filter { it.startsWith(args[1], ignoreCase = true) }
                "stat"    -> listOf("info","reset","초기화권").filter { it.startsWith(args[1], ignoreCase = true) }
                "weapon"  -> listOf("rank","damage").filter { it.startsWith(args[1], ignoreCase = true) }
                "armor"   -> listOf("rank").filter { it.startsWith(args[1], ignoreCase = true) }
                "upgrade" -> listOf("파괴방어","하락방어","강화석").filter { it.startsWith(args[1], ignoreCase = true) }
                "jewelry" -> listOf("low","mid","high","supreme").filter { it.startsWith(args[1], ignoreCase = true) }
                "awake"   -> listOf("scroll","stone").filter { it.startsWith(args[1], ignoreCase = true) }
                else      -> emptyList()
            }
            3 -> when {
                args[0].lowercase() == "weapon" && args[1].lowercase() == "rank" -> gradeIds.filter { it.startsWith(args[2], ignoreCase = true) }
                args[0].lowercase() == "armor"  && args[1].lowercase() == "rank" -> gradeIds.filter { it.startsWith(args[2], ignoreCase = true) }
                args[0].lowercase() == "level"  && args[1].lowercase() in listOf("info","setlevel","setxp","givexp","초기화권") -> online.filter { it.startsWith(args[2], ignoreCase = true) }
                args[0].lowercase() == "stat"   && args[1].lowercase() in listOf("info","reset","초기화권") -> online.filter { it.startsWith(args[2], ignoreCase = true) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
