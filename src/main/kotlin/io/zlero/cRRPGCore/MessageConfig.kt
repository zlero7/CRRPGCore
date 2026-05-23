package io.zlero.cRRPGCore

import org.bukkit.configuration.file.FileConfiguration

/**
 * config.yml 의 messages / items 섹션을 한 곳에서 관리합니다.
 * CRRPGCorePlugin.msgCfg 로 접근하세요.
 */
class MessageConfig(private val plugin: CRRPGCorePlugin) {

    /* ══════════════════════════════════════════════════════════
       공통 메시지
    ══════════════════════════════════════════════════════════ */

    // 공통
    var errPlayerOnly        = "§c플레이어만 사용 가능합니다."
    var errNoPermission      = "§c[!] §c권한이 없습니다."
    var errPlayerNotFound    = "§c[!] §c플레이어 §e{player}§c를 찾을 수 없습니다."
    var errConsoleNoPlayer   = "§c[!] §c콘솔에서는 플레이어 이름을 지정하세요."
    var errConsoleUnavail    = "§c[!] §c콘솔에서는 사용할 수 없습니다."
    var errNeedItemInHand    = "§c손에 아이템을 들고 사용하세요."
    var errNeedWeaponInHand  = "§c[!] 손에 무기를 들고 사용하세요."

    // 감정
    var errNotRpgItem          = "§cRPG 아이템이 아닙니다."
    var errAlreadyAppraised    = "§c이미 감정된 아이템입니다."
    var errNotAppraised        = "§c감정되지 않은 아이템입니다. §7먼저 §f/감정 §7을 사용하세요."
    var errNotEnoughMoneyApp   = "§c감정 비용이 부족합니다. §8(필요: §e{cost}원§8)"
    var errNotEnoughMoneyReapp = "§c재감정 비용이 부족합니다. §8(필요: §e{cost}원§8)"
    var errNoSocket            = "§c각성이 존재하지않습니다."
    var errAppraisalMaxReached = "§c감정 최대 리롤 횟수에 도달했습니다."
    var errAppraisalFail       = "§c감정 리롤 실패."
    var errNotAppraisedCmdFail = "§c감정 실패. RPG 무기/방어구만 감정 가능합니다."
    var msgAppraisalSuccess    = "§6[ 감정 ] §f{grade} 감정 완료! §8(§e{slots}§8종 스텟 부여, 재감정 §7{remaining}§8)  §7-{cost}원"
    var msgReappraisalSuccess  = "§6[ 재감정 ] §f{slots}종 스텟이 새로 결정되었습니다! §8(감정 §7{remaining}§8)  §7-{cost}원"

    // 각성
    var errAlreadyAwakened       = "§c이미 각성된 아이템입니다."
    var errNotAwakened           = "§c각성이 되지 않은 아이템입니다. 먼저 각성석을 사용하세요."
    var errSocketMaxReached      = "§c재각성 횟수를 모두 사용했습니다."
    var errNotEnoughMoneySocket  = "§c잔액이 부족합니다. §8(필요: §e{cost}원§8)"
    var msgSocketSuccess         = "§b[각성] §f각성 완료! §8(§e{slots}§8 슬롯) §7-{cost}원"
    var msgSocketRerollSuccess   = "§b[재각성] §f{slots}슬롯으로 재각성! §7-{cost}원"

    // 강화
    var errNeedItem            = "§c[!] 강화할 아이템을 올려주세요."
    var errNotRpgItemUpgrade   = "§c[!] RPG 아이템만 강화할 수 있습니다."
    var errNeedStone           = "§c[!] 강화석을 올려주세요."
    var errMaxUpgrade          = "§c[!] 이미 최대 강화 상태입니다."
    var errWrongStone          = "§c[!] 잘못된 강화석입니다. {required}§c이 필요합니다."
    var msgUpgradeSuccess      = "§6⚒ §a강화 성공! §f+{prev} §8→ §6+{new}"
    var msgUpgradeFail         = "§7⚒ 강화 실패. §f강화 단계 유지. §8(+{prev})"
    var msgUpgradeDown         = "§e⚒ 강화 실패! §f강화 단계가 §e-1 하락§f했습니다. §8(+{prev} → +{new})"
    var msgUpgradeBreak        = "§c⚒ 강화 실패! §f아이템이 §c파괴§f되었습니다."

    // 보석
    var errJewelOnly           = "§c보석에는 감정서만 사용 가능합니다."
    var errJewelAlreadyApp     = "§c이미 감정된 보석입니다."
    var errJewelNotEnoughMoney = "§c잔액이 부족합니다. §8(필요: §e{cost}원§8)"
    var msgJewelAppraisalOk    = "§6[보석 감정] §f감정 완료! §7-{cost}원"
    var msgJewelReappraisalOk  = "§6[보석 재감정] §f스텟이 새로 결정되었습니다! §7-{cost}원"
    var errJewelAppraisalFail  = "§c보석 감정 실패."
    var errJewelReappraisalMax = "§c보석 재감정 횟수를 모두 사용했습니다."

    // 루(보석 장착)
    var errJewelOnly2        = "§c[!] 보석만 장착할 수 있습니다."
    var errNotAppraisedJewel = "§c[!] 감정되지 않은 보석입니다. §e/감정§c 명령어를 먼저 사용하세요."
    var errRoonFull          = "§c[!] 루 슬롯이 모두 가득 찼습니다. (9/9)"
    var msgRoonEquip         = "§6✦ §f보석을 루 슬롯 {slot}번에 장착했습니다."
    var msgRoonUnequip       = "§7✦ §f보석을 루 슬롯 {slot}번에서 해제했습니다."

    // 스텟 / 전투
    var errStatMaxReached    = "§c[!] §c{stat} 스텟이 최대치(§e{max}§c)입니다!"
    var msgDodgeSuccess      = "§b[!] §f회피 성공"
    var errWeaponNoDamage    = "§c[!] 무기 기본 데미지가 설정되지 않았습니다."
    var msgCombatCrit        = "§e✦ 치명타!"
    var msgCombatOut         = "§8[§aOUT§8] §f{target} §c{damage}§f데미지{crit}  §8(기본:{base} 강화:+{upg} 감정:+{appr} 보석:+{jewel} 힘:+{str})"
    var msgArmorEvasion      = "§b[!] §f장비 회피 성공!"
    var msgArmorEvasionFoe   = "§8[§bOUT§8] §b{target}§7이(가) 회피했습니다."
    var msgLifeSteal         = "§c♥ 흡혈! §f+{amount}"
    var msgCombatIn          = "§8[§cIN§8] §f{attacker} §c{damage}§f피해  §8(방어:{defense}% 관통:{pen}%)"
    var showCombatOut        = true
    var showCombatIn         = true

    // 스텟 초기화권
    var errNoStatToReset     = "§c초기화할 스텟이 없습니다!"
    var msgStatResetOk       = "§6✦ §e스텟 초기화권§f을 사용했습니다!"
    var msgStatResetDetail   = "§7초기화된 포인트 §8: §a+{points} §7/ 잔여 §8: §b{remaining}점"

    // 레벨 초기화권
    var errNoLevelToReset    = "§c[!] §c초기화할 레벨 또는 스텟이 없습니다!"
    var msgLevelResetOk      = "§5[!] §5✦ §d레벨 초기화권§f을 사용했습니다!"

    // 경험치 부스트
    var errGlobalBoostActive   = "§c[!] 이미 전체 경험치 부스트가 활성화 중입니다."
    var errPersonalBoostActive = "§c[!] 이미 개인 경험치 부스트가 활성화 중입니다."
    var msgGlobalBoostActivated= "§6[!] §e{player}§f이(가) §6전체 경험치 부스트 §e{mult}x §f(§b{min}분§f)를 활성화했습니다!"
    var msgPersonalBoostActivated= "§6[!] §f개인 경험치 부스트 §e{mult}x §f(§b{min}분§f)가 활성화되었습니다!"

    // 관리자 명령 공통
    var msgAdminLevelSet      = "§a[!] §a{player}의 레벨을 §e{level}§a으로 설정했습니다."
    var msgAdminLevelSetPlayer= "§a[!] §a관리자에 의해 레벨이 §e{level}§a으로 설정되었습니다."
    var msgAdminXpSet         = "§a[!] §a{player}의 XP를 §e{xp}§a으로 설정했습니다."
    var msgAdminXpGive        = "§a[!] §a{player}에게 §e{xp} XP§a를 지급했습니다."
    var msgAdminReload        = "§a[!] §aCRRPGCore 설정을 다시 불러왔습니다."
    var msgAdminStatReset     = "§a[!] §a{player}의 스텟을 초기화했습니다. 잔여 포인트: §e{points}"
    var msgAdminStatResetPlayer= "§a[!] §a관리자에 의해 스텟이 초기화되었습니다. 잔여 포인트: §e{points}"
    var msgAdminWeaponRank    = "§a[!] §f손에 든 §e{material}§a을 §f{grade} §a으로 설정했습니다."
    var msgAdminWeaponRankTip = "§7플레이어에게 §e/감정 §7명령어로 스텟을 부여받도록 안내하세요."
    var msgAdminWeaponDamage  = "§a[!] §f손에 든 무기의 기본 데미지를 §c{damage}§a으로 설정했습니다.{upgNote}"
    var msgAdminGiveScroll    = "§a[!] §a{player}에게 {item} §f{count}개§a를 지급했습니다."
    var msgReceiveScroll      = "§e[!] {item} §f{count}개§e를 받았습니다! §7(우클릭하여 사용)"
    var msgAdminGiveStone     = "§a[!] §a{player}에게 §f{grade} 강화석 {count}개§a를 지급했습니다."
    var msgReceiveStone       = "§e[!] §f{grade} 강화석 §f{count}개§e를 받았습니다!"
    var msgAdminGiveJewel     = "§a[!] {player}에게 {grade} 보석 {count}개§a를 지급했습니다."
    var msgReceiveJewel       = "§e[!] {grade} 보석 §f{count}개§e를 받았습니다! §7(/감정으로 감정하세요)"
    var msgStatPoints         = "§e[!] §f스텟 포인트 §a{points}P §f지급되었습니다"

    /* ══════════════════════════════════════════════════════════
       아이템 설정 (이름 / 로어 / 재질 / Custom Model Data)
    ══════════════════════════════════════════════════════════ */

    // 감정서
    var scrollName         = "§6감정서"
    var scrollLore         = listOf("§7각성/감정 GUI에서 사용하세요", "§8(/rpgcore awake)")
    var scrollMaterial     = "PAPER"
    var scrollCustomModel  = 0

    // 각성석
    var stoneName          = "§b각성석"
    var stoneLore          = listOf("§7각성/감정 GUI에서 사용하세요", "§8(/rpgcore awake)")
    var stoneMaterial      = "AMETHYST_SHARD"
    var stoneCustomModel   = 0

    // 강화석
    var upgStoneLowName    = "§f✦ 하급 강화석"
    var upgStoneLowLore    = listOf("§r", "  §7+1 ~ +3 강화에 사용합니다.", "§r")
    var upgStoneLowMat     = "COBBLESTONE"
    var upgStoneLowCMD     = 0

    var upgStoneMidName    = "§b✦ 중급 강화석"
    var upgStoneMidLore    = listOf("§r", "  §7+4 ~ +7 강화에 사용합니다.", "§r")
    var upgStoneMidMat     = "LAPIS_LAZULI"
    var upgStoneMidCMD     = 0

    var upgStoneHighName   = "§5✦ 상급 강화석"
    var upgStoneHighLore   = listOf("§r", "  §7+8 ~ +10 강화에 사용합니다.", "§r")
    var upgStoneHighMat    = "AMETHYST_SHARD"
    var upgStoneHighCMD    = 0

    // 강화 방지권
    var protectBreakName   = "§c✦ 파괴방지권"
    var protectBreakLore   = listOf("§r", "  §7강화 실패 시 §c파괴§7를 막습니다.", "  §7파괴 확률이 §f실패§7로 전환됩니다.", "§r")
    var protectBreakMat    = "TOTEM_OF_UNDYING"
    var protectBreakCMD    = 0

    var protectDownName    = "§e✦ 하락방지권"
    var protectDownLore    = listOf("§r", "  §7강화 실패 시 §e하락§7을 막습니다.", "  §7하락 확률이 §f실패§7로 전환됩니다.", "§r")
    var protectDownMat     = "SHIELD"
    var protectDownCMD     = 0

    // 스텟 초기화권
    var statResetScrollName  = "§6✦ §e스텟 초기화권 §6✦"
    var statResetScrollLore  = listOf(
        "§r", "  §7우클릭하여 사용", "§r",
        "  §8✦ §f힘 §8/ §f체력 §8/ §f민첩 §7스텟을 초기화하고",
        "  §8  §7투자한 포인트를 전액 환급합니다.", "§r",
        "  §c주의: §7사용 즉시 효과가 적용됩니다."
    )
    var statResetScrollMat   = "PAPER"
    var statResetScrollCMD   = 0

    // 레벨 초기화권
    var levelResetScrollName  = "§5✦ §d레벨 초기화권 §5✦"
    var levelResetScrollLore  = listOf(
        "§r", "  §7우클릭하여 사용", "§r",
        "  §8✦ §f레벨 §8/ §fXP §8/ §f스텟§7을 모두 초기화합니다.",
        "  §8  §7레벨은 §e[1]§7로, 스텟 포인트는 §e[1]레벨 기본값§7으로 돌아갑니다.",
        "§r", "  §c주의: §7사용 즉시 효과가 적용됩니다."
    )
    var levelResetScrollMat   = "PAPER"
    var levelResetScrollCMD   = 0

    // 경험치 부스트권 (이름 템플릿, {scope}/{mult}/{min} 치환)
    var xpBoostScrollName     = "§6✦ 경험치 부스트권 {scope}"
    var xpBoostScrollLore     = listOf(
        "§r",
        "  §7배수 §8: §e{mult}x",
        "  §7시간 §8: §b{min}분",
        "  §7범위 §8: {scope}",
        "§r",
        "  §f우클릭하여 사용"
    )
    var xpBoostScrollMat      = "PAPER"
    var xpBoostScrollCMD      = 0
    var xpBoostScopePersonal  = "§a[개인]"
    var xpBoostScopeGlobal    = "§c[전체]"

    /* ══════════════════════════════════════════════════════════
       로어 텍스트 (SocketManager.rebuildOurBlock 에서 사용)
    ══════════════════════════════════════════════════════════ */
    var loreGradeLabel        = "◆ §f등급 §8: "   // 뒤에 등급 색+이름 붙음
    var loreSeparator         = "§8──────────────────"
    var loreWeaponDmgLabel    = "  §c⚔ §f무기 데미지 §8: §c+"  // 뒤에 값 붙음
    var loreNoSocket          = "  §7각성석으로 각성을 진행해주세요"
    var loreEmptySlot         = "  §8[ §7🔒 §8] §7빈 인챈트 슬롯"
    var loreAppraisalGuide    = "  §7▷ /rpgcore awake 으로 감정을 진행해주세요"
    var loreSocketRemain      = "  §8[ §7남은 재각성 횟수 §8: {count} §8]"
    var loreAppraisalRemain   = "  §8[ §7남은 재감정 횟수 §8: {count} §8]"
    var loreAppraised         = "  §8[§a감정된 아이템§8]"
    var loreInfSymbol         = "§a∞"

    /* ══════════════════════════════════════════════════════════
       로어 텍스트 (무기/방어구 스텟 라인)
    ══════════════════════════════════════════════════════════ */
    // 무기
    var weaponStatDmg      = "§c공격력 §f>> §7"
    var weaponStatCrit     = "§e치명타 확률 §f>> §7"
    var weaponStatCritDmg  = "§e치명타 피해 §f>> §7"
    var weaponStatAtkSpd   = "§a공격 속도 §f>> §7"
    var weaponStatPen      = "§6방어 관통 §f>> §7"
    var weaponStatLS       = "§d생명력 흡수 §f>> §7"
    // 방어구
    var armorStatHp        = "§b최대 체력 §f>> §7"
    var armorStatDef       = "§3방어력 §f>> §7"
    var armorStatEva       = "§2회피율 §f>> §7"

    /* ══════════════════════════════════════════════════════════
       감정 스텟 라벨 (AppraisalManager.rollStats 에서 사용)
    ══════════════════════════════════════════════════════════ */
    // 무기 감정 스텟
    var apprWeapDmgLabel   = "§c추가 데미지"
    var apprWeapCritLabel  = "§e치명타 확률"
    var apprWeapCritDLabel = "§e치명타 피해"
    var apprWeapSpdLabel   = "§a공격 속도  "
    var apprWeapPenLabel   = "§b관통       "
    var apprWeapLsLabel    = "§c흡혈       "
    // 방어구 감정 스텟
    var apprArmHpLabel     = "§a추가 생명력"
    var apprArmDefLabel    = "§b방어력     "
    var apprArmEvaLabel    = "§e회피율     "

    /* ══════════════════════════════════════════════════════════
       AwakeGui 내부 텍스트
    ══════════════════════════════════════════════════════════ */
    var guiAwakeTitle          = "§8[ §6각성 §8/ §e감정 §8]"
    var guiAwakeSlotWeapon     = "§e무기 / 장비 슬롯"
    var guiAwakeSlotWeaponLore = listOf("§7RPG 무기 또는 장비를 올려주세요")
    var guiAwakeSlotScroll     = "§a스크롤 슬롯"
    var guiAwakeSlotScrollLore = listOf("§6감정서 §7또는 §b각성석§7을 올려주세요")
    var guiAwakePending        = "§711번과 13번 슬롯을 채워주세요"
    var guiAwakeActionOk       = "§a클릭하여 진행"
    var guiAwakeNoMoney        = "§c잔액 부족"
    var guiAwakeLabelAction    = "§7동작: "
    var guiAwakeLabelCost      = "§7비용: §e{cost}원"
    var guiAwakeLabelNoMoney   = "§c잔액이 부족합니다!"
    var guiAwakeLabelOk        = "§a진행 가능"
    var guiAwakeLabelAppraise  = "§6최초 감정"
    var guiAwakeLabelReappraise= "§e재감정"
    var guiAwakeLabelAwake     = "§a최초 각성"
    var guiAwakeLabelReawake   = "§b재각성"
    // AwakeGui 처리 결과 메시지
    var msgAwakeAppraisalOk    = "§6[감정] §f감정 완료! §7-{cost}원"
    var msgAwakeReappraisalOk  = "§6[재감정] §f스텟이 새로 결정되었습니다! §7-{cost}원"
    var msgAwakeSocketOk       = "§b[각성] §f각성 완료! §8(§e{slots}§8 슬롯) §7-{cost}원"
    var msgAwakeSocketRerollOk = "§b[재각성] §f{slots}슬롯으로 재각성! §7-{cost}원"
    var msgAwakeSlotHint11     = "§c11번 슬롯에 아이템을 올려주세요."
    var msgAwakeSlotHint13     = "§c13번 슬롯에 감정서 또는 각성석을 올려주세요."
    var errAwakeWrongScroll    = "§c올바른 스크롤이 아닙니다. §7(§6감정서 §7또는 §b각성석§7)"
    var errAwakeNotRpgItem     = "§cRPG 아이템이 아닙니다."
    var errAwakeApprMaxReached = "§c재감정 횟수를 모두 사용했습니다."
    var errAwakeSockMaxReached = "§c재각성 횟수를 모두 사용했습니다."

    /* ══════════════════════════════════════════════════════════
       UpgradeGui 내부 텍스트
    ══════════════════════════════════════════════════════════ */
    var guiUpgradeTitle         = "§8⚒ §6강화 §8⚒"
    var guiUpgradeSlotItemName  = "§a[ 강화할 아이템 ]"
    var guiUpgradeSlotItemLore  = listOf("§7인벤에서 RPG 아이템을 클릭하세요.")
    var guiUpgradeSlotStoneName = "§e[ 강화석 ]"
    var guiUpgradeSlotStoneLore = listOf("§7인벤에서 강화석을 클릭하세요.", "§r",
        "§f+1~+3 §8: §f하급 강화석", "§b+4~+7 §8: §b중급 강화석", "§5+8~+10 §8: §5상급 강화석")
    var guiUpgradeSlotBreakName = "§c[ 파괴방지권 ]"
    var guiUpgradeSlotBreakLore = listOf("§7선택 — 파괴방지권을 클릭하세요.")
    var guiUpgradeSlotDownName  = "§e[ 하락방지권 ]"
    var guiUpgradeSlotDownLore  = listOf("§7선택 — 하락방지권을 클릭하세요.")
    var guiUpgradeSlotResultName= "§f[ 결과 ]"
    var guiUpgradeSlotResultLore= listOf("§7강화 후 결과 아이템이 여기에 나타납니다.", "§7클릭하여 가져가세요.")
    var guiUpgradeBtnNoItem     = "§7강화하기"
    var guiUpgradeBtnNoItemLore = listOf("§7강화할 아이템을 올려주세요.")
    var guiUpgradeBtnNotRpg     = "§c강화 불가"
    var guiUpgradeBtnNotRpgLore = listOf("§7감정된 RPG 아이템만 강화할 수 있습니다.")
    var guiUpgradeBtnMax        = "§6★ 최대 강화"
    var guiUpgradeBtnMaxLore    = listOf("§7이미 +10 최대 강화 상태입니다.")
    var guiUpgradeBtnReady      = "§6⚒ 강화하기  §8(+{cur} → +{tgt})"
    var guiUpgradeBtnNotReady   = "§c강화 불가"
    var guiUpgradeBtnLoreStoneOk   = "  §a▶ 좌클릭하여 강화!"
    var guiUpgradeBtnLoreStoneErr  = "  §c✖ 강화석 종류가 맞지 않습니다"

    /* ══════════════════════════════════════════════════════════
       RoonGui 내부 텍스트
    ══════════════════════════════════════════════════════════ */
    var guiRoonTitle         = "§8✦ §6루 장착 §8✦"
    var guiRoonEmptySlotName = "§8[ 빈 슬롯 {number} ]"
    var guiRoonEmptySlotLore = listOf("§7보석을 클릭하여 장착하세요.")

    /* ══════════════════════════════════════════════════════════
       필요 강화석 이름 (UpgradeManager.requiredStoneName)
    ══════════════════════════════════════════════════════════ */
    var stoneNameLow  = "§f하급 강화석"
    var stoneNameMid  = "§b중급 강화석"
    var stoneNameHigh = "§5상급 강화석"

    // ─────────────────────────────────────────────────────────────────
    fun load(config: FileConfiguration) {
        val m = "messages"
        val i = "items"

        // ── 공통 메시지 ──
        errPlayerOnly        = config.getString("$m.err-player-only",         errPlayerOnly)!!
        errNoPermission      = config.getString("$m.err-no-permission",       errNoPermission)!!
        errPlayerNotFound    = config.getString("$m.err-player-not-found",    errPlayerNotFound)!!
        errConsoleNoPlayer   = config.getString("$m.err-console-no-player",   errConsoleNoPlayer)!!
        errConsoleUnavail    = config.getString("$m.err-console-unavail",      errConsoleUnavail)!!
        errNeedItemInHand    = config.getString("$m.err-need-item-in-hand",   errNeedItemInHand)!!
        errNeedWeaponInHand  = config.getString("$m.err-need-weapon-in-hand", errNeedWeaponInHand)!!

        // 감정
        errNotRpgItem          = config.getString("$m.err-not-rpg-item",           errNotRpgItem)!!
        errAlreadyAppraised    = config.getString("$m.err-already-appraised",       errAlreadyAppraised)!!
        errNotAppraised        = config.getString("$m.err-not-appraised",           errNotAppraised)!!
        errNotEnoughMoneyApp   = config.getString("$m.err-not-enough-money-app",    errNotEnoughMoneyApp)!!
        errNotEnoughMoneyReapp = config.getString("$m.err-not-enough-money-reapp",  errNotEnoughMoneyReapp)!!
        errNoSocket            = config.getString("$m.err-no-socket",               errNoSocket)!!
        errAppraisalMaxReached = config.getString("$m.err-appraisal-max-reached",   errAppraisalMaxReached)!!
        errAppraisalFail       = config.getString("$m.err-appraisal-fail",          errAppraisalFail)!!
        errNotAppraisedCmdFail = config.getString("$m.err-not-appraised-cmd-fail",  errNotAppraisedCmdFail)!!
        msgAppraisalSuccess    = config.getString("$m.appraisal-success",           msgAppraisalSuccess)!!
        msgReappraisalSuccess  = config.getString("$m.reappraisal-success",         msgReappraisalSuccess)!!

        // 각성
        errAlreadyAwakened       = config.getString("$m.err-already-awakened",       errAlreadyAwakened)!!
        errNotAwakened           = config.getString("$m.err-not-awakened",           errNotAwakened)!!
        errSocketMaxReached      = config.getString("$m.err-socket-max-reached",     errSocketMaxReached)!!
        errNotEnoughMoneySocket  = config.getString("$m.err-not-enough-money-socket",errNotEnoughMoneySocket)!!
        msgSocketSuccess         = config.getString("$m.socket-success",             msgSocketSuccess)!!
        msgSocketRerollSuccess   = config.getString("$m.socket-reroll-success",      msgSocketRerollSuccess)!!

        // 강화
        errNeedItem           = config.getString("$m.err-need-item",           errNeedItem)!!
        errNotRpgItemUpgrade  = config.getString("$m.err-not-rpg-item-upgrade",errNotRpgItemUpgrade)!!
        errNeedStone          = config.getString("$m.err-need-stone",           errNeedStone)!!
        errMaxUpgrade         = config.getString("$m.err-max-upgrade",          errMaxUpgrade)!!
        errWrongStone         = config.getString("$m.err-wrong-stone",          errWrongStone)!!
        msgUpgradeSuccess     = config.getString("$m.upgrade-success",          msgUpgradeSuccess)!!
        msgUpgradeFail        = config.getString("$m.upgrade-fail",             msgUpgradeFail)!!
        msgUpgradeDown        = config.getString("$m.upgrade-down",             msgUpgradeDown)!!
        msgUpgradeBreak       = config.getString("$m.upgrade-break",            msgUpgradeBreak)!!

        // 보석
        errJewelOnly           = config.getString("$m.err-jewel-only",           errJewelOnly)!!
        errJewelAlreadyApp     = config.getString("$m.err-jewel-already-app",    errJewelAlreadyApp)!!
        errJewelNotEnoughMoney = config.getString("$m.err-jewel-not-enough-money", errJewelNotEnoughMoney)!!
        msgJewelAppraisalOk    = config.getString("$m.jewel-appraisal-ok",        msgJewelAppraisalOk)!!
        msgJewelReappraisalOk  = config.getString("$m.jewel-reappraisal-ok",      msgJewelReappraisalOk)!!
        errJewelAppraisalFail  = config.getString("$m.err-jewel-appraisal-fail",  errJewelAppraisalFail)!!
        errJewelReappraisalMax = config.getString("$m.err-jewel-reappraisal-max", errJewelReappraisalMax)!!

        // 루
        errJewelOnly2        = config.getString("$m.err-jewel-only-2",       errJewelOnly2)!!
        errNotAppraisedJewel = config.getString("$m.err-not-appraised-jewel",errNotAppraisedJewel)!!
        errRoonFull          = config.getString("$m.err-roon-full",           errRoonFull)!!
        msgRoonEquip         = config.getString("$m.roon-equip",              msgRoonEquip)!!
        msgRoonUnequip       = config.getString("$m.roon-unequip",            msgRoonUnequip)!!

        // 스텟 / 전투
        errStatMaxReached  = config.getString("$m.err-stat-max-reached",  errStatMaxReached)!!
        msgDodgeSuccess    = config.getString("$m.dodge-success",          msgDodgeSuccess)!!
        errWeaponNoDamage  = config.getString("$m.err-weapon-no-damage",   errWeaponNoDamage)!!
        msgCombatCrit      = config.getString("$m.combat-crit",            msgCombatCrit)!!
        msgCombatOut       = config.getString("$m.combat-out",             msgCombatOut)!!
        msgArmorEvasion    = config.getString("$m.armor-evasion",          msgArmorEvasion)!!
        msgArmorEvasionFoe = config.getString("$m.armor-evasion-foe",      msgArmorEvasionFoe)!!
        msgLifeSteal       = config.getString("$m.life-steal",             msgLifeSteal)!!
        msgCombatIn        = config.getString("$m.combat-in",              msgCombatIn)!!
        showCombatOut      = config.getBoolean("combat.show-out-message",  showCombatOut)
        showCombatIn       = config.getBoolean("combat.show-in-message",   showCombatIn)

        // 스텟 초기화권
        errNoStatToReset   = config.getString("$m.err-no-stat-to-reset",  errNoStatToReset)!!
        msgStatResetOk     = config.getString("$m.stat-reset-ok",          msgStatResetOk)!!
        msgStatResetDetail = config.getString("$m.stat-reset-detail",      msgStatResetDetail)!!

        // 레벨 초기화권
        errNoLevelToReset  = config.getString("$m.err-no-level-to-reset", errNoLevelToReset)!!
        msgLevelResetOk    = config.getString("$m.level-reset-ok",         msgLevelResetOk)!!

        // XP 부스트
        errGlobalBoostActive    = config.getString("$m.err-global-boost-active",     errGlobalBoostActive)!!
        errPersonalBoostActive  = config.getString("$m.err-personal-boost-active",   errPersonalBoostActive)!!
        msgGlobalBoostActivated = config.getString("$m.global-boost-activated",      msgGlobalBoostActivated)!!
        msgPersonalBoostActivated= config.getString("$m.personal-boost-activated",   msgPersonalBoostActivated)!!

        // 관리자
        msgAdminLevelSet       = config.getString("$m.admin-level-set",        msgAdminLevelSet)!!
        msgAdminLevelSetPlayer = config.getString("$m.admin-level-set-player", msgAdminLevelSetPlayer)!!
        msgAdminXpSet          = config.getString("$m.admin-xp-set",           msgAdminXpSet)!!
        msgAdminXpGive         = config.getString("$m.admin-xp-give",          msgAdminXpGive)!!
        msgAdminReload         = config.getString("$m.admin-reload",            msgAdminReload)!!
        msgAdminStatReset      = config.getString("$m.admin-stat-reset",        msgAdminStatReset)!!
        msgAdminStatResetPlayer= config.getString("$m.admin-stat-reset-player", msgAdminStatResetPlayer)!!
        msgAdminWeaponRank     = config.getString("$m.admin-weapon-rank",       msgAdminWeaponRank)!!
        msgAdminWeaponRankTip  = config.getString("$m.admin-weapon-rank-tip",   msgAdminWeaponRankTip)!!
        msgAdminWeaponDamage   = config.getString("$m.admin-weapon-damage",     msgAdminWeaponDamage)!!
        msgAdminGiveScroll     = config.getString("$m.admin-give-scroll",       msgAdminGiveScroll)!!
        msgReceiveScroll       = config.getString("$m.receive-scroll",          msgReceiveScroll)!!
        msgAdminGiveStone      = config.getString("$m.admin-give-stone",        msgAdminGiveStone)!!
        msgReceiveStone        = config.getString("$m.receive-stone",           msgReceiveStone)!!
        msgAdminGiveJewel      = config.getString("$m.admin-give-jewel",        msgAdminGiveJewel)!!
        msgReceiveJewel        = config.getString("$m.receive-jewel",           msgReceiveJewel)!!
        msgStatPoints          = config.getString("$m.stat-points",             msgStatPoints)!!

        // ── 아이템 ──
        scrollName        = config.getString("$i.scroll.name",      scrollName)!!
        scrollLore        = config.getStringList("$i.scroll.lore").ifEmpty { scrollLore }
        scrollMaterial    = config.getString("$i.scroll.material",  scrollMaterial)!!
        scrollCustomModel = config.getInt("$i.scroll.custom-model-data", 0)

        stoneName         = config.getString("$i.awake-stone.name",     stoneName)!!
        stoneLore         = config.getStringList("$i.awake-stone.lore").ifEmpty { stoneLore }
        stoneMaterial     = config.getString("$i.awake-stone.material", stoneMaterial)!!
        stoneCustomModel  = config.getInt("$i.awake-stone.custom-model-data", 0)

        upgStoneLowName   = config.getString("$i.upgrade-stone-low.name",     upgStoneLowName)!!
        upgStoneLowLore   = config.getStringList("$i.upgrade-stone-low.lore").ifEmpty { upgStoneLowLore }
        upgStoneLowMat    = config.getString("$i.upgrade-stone-low.material", upgStoneLowMat)!!
        upgStoneLowCMD    = config.getInt("$i.upgrade-stone-low.custom-model-data", 0)

        upgStoneMidName   = config.getString("$i.upgrade-stone-mid.name",     upgStoneMidName)!!
        upgStoneMidLore   = config.getStringList("$i.upgrade-stone-mid.lore").ifEmpty { upgStoneMidLore }
        upgStoneMidMat    = config.getString("$i.upgrade-stone-mid.material", upgStoneMidMat)!!
        upgStoneMidCMD    = config.getInt("$i.upgrade-stone-mid.custom-model-data", 0)

        upgStoneHighName  = config.getString("$i.upgrade-stone-high.name",     upgStoneHighName)!!
        upgStoneHighLore  = config.getStringList("$i.upgrade-stone-high.lore").ifEmpty { upgStoneHighLore }
        upgStoneHighMat   = config.getString("$i.upgrade-stone-high.material", upgStoneHighMat)!!
        upgStoneHighCMD   = config.getInt("$i.upgrade-stone-high.custom-model-data", 0)

        protectBreakName  = config.getString("$i.protect-break.name",     protectBreakName)!!
        protectBreakLore  = config.getStringList("$i.protect-break.lore").ifEmpty { protectBreakLore }
        protectBreakMat   = config.getString("$i.protect-break.material", protectBreakMat)!!
        protectBreakCMD   = config.getInt("$i.protect-break.custom-model-data", 0)

        protectDownName   = config.getString("$i.protect-down.name",     protectDownName)!!
        protectDownLore   = config.getStringList("$i.protect-down.lore").ifEmpty { protectDownLore }
        protectDownMat    = config.getString("$i.protect-down.material", protectDownMat)!!
        protectDownCMD    = config.getInt("$i.protect-down.custom-model-data", 0)

        statResetScrollName  = config.getString("$i.stat-reset-scroll.name",     statResetScrollName)!!
        statResetScrollLore  = config.getStringList("$i.stat-reset-scroll.lore").ifEmpty { statResetScrollLore }
        statResetScrollMat   = config.getString("$i.stat-reset-scroll.material", statResetScrollMat)!!
        statResetScrollCMD   = config.getInt("$i.stat-reset-scroll.custom-model-data", 0)

        levelResetScrollName  = config.getString("$i.level-reset-scroll.name",     levelResetScrollName)!!
        levelResetScrollLore  = config.getStringList("$i.level-reset-scroll.lore").ifEmpty { levelResetScrollLore }
        levelResetScrollMat   = config.getString("$i.level-reset-scroll.material", levelResetScrollMat)!!
        levelResetScrollCMD   = config.getInt("$i.level-reset-scroll.custom-model-data", 0)

        xpBoostScrollName     = config.getString("$i.xp-boost-scroll.name",     xpBoostScrollName)!!
        xpBoostScrollLore     = config.getStringList("$i.xp-boost-scroll.lore").ifEmpty { xpBoostScrollLore }
        xpBoostScrollMat      = config.getString("$i.xp-boost-scroll.material", xpBoostScrollMat)!!
        xpBoostScrollCMD      = config.getInt("$i.xp-boost-scroll.custom-model-data", 0)
        xpBoostScopePersonal  = config.getString("$i.xp-boost-scroll.scope-personal", xpBoostScopePersonal)!!
        xpBoostScopeGlobal    = config.getString("$i.xp-boost-scroll.scope-global",   xpBoostScopeGlobal)!!

        // ── 로어 텍스트 ──
        val l = "lore-text"
        loreGradeLabel      = config.getString("$l.grade-label",       loreGradeLabel)!!
        loreSeparator       = config.getString("$l.separator",         loreSeparator)!!
        loreWeaponDmgLabel  = config.getString("$l.weapon-dmg-label",  loreWeaponDmgLabel)!!
        loreNoSocket        = config.getString("$l.no-socket",         loreNoSocket)!!
        loreEmptySlot       = config.getString("$l.empty-slot",        loreEmptySlot)!!
        loreAppraisalGuide  = config.getString("$l.appraisal-guide",   loreAppraisalGuide)!!
        loreSocketRemain    = config.getString("$l.socket-remain",      loreSocketRemain)!!
        loreAppraisalRemain = config.getString("$l.appraisal-remain",   loreAppraisalRemain)!!
        loreAppraised       = config.getString("$l.appraised",         loreAppraised)!!
        loreInfSymbol       = config.getString("$l.inf-symbol",        loreInfSymbol)!!

        val ws = "lore-text.weapon-stat"
        weaponStatDmg     = config.getString("$ws.damage",      weaponStatDmg)!!
        weaponStatCrit    = config.getString("$ws.crit-chance", weaponStatCrit)!!
        weaponStatCritDmg = config.getString("$ws.crit-damage", weaponStatCritDmg)!!
        weaponStatAtkSpd  = config.getString("$ws.atk-speed",   weaponStatAtkSpd)!!
        weaponStatPen     = config.getString("$ws.penetration", weaponStatPen)!!
        weaponStatLS      = config.getString("$ws.life-steal",  weaponStatLS)!!

        val as_ = "lore-text.armor-stat"
        armorStatHp  = config.getString("$as_.health",  armorStatHp)!!
        armorStatDef = config.getString("$as_.defense", armorStatDef)!!
        armorStatEva = config.getString("$as_.evasion", armorStatEva)!!

        val ap = "lore-text.appraisal-stat"
        apprWeapDmgLabel   = config.getString("$ap.weapon-damage",     apprWeapDmgLabel)!!
        apprWeapCritLabel  = config.getString("$ap.weapon-crit",       apprWeapCritLabel)!!
        apprWeapCritDLabel = config.getString("$ap.weapon-crit-dmg",   apprWeapCritDLabel)!!
        apprWeapSpdLabel   = config.getString("$ap.weapon-atk-speed",  apprWeapSpdLabel)!!
        apprWeapPenLabel   = config.getString("$ap.weapon-pen",        apprWeapPenLabel)!!
        apprWeapLsLabel    = config.getString("$ap.weapon-life-steal", apprWeapLsLabel)!!
        apprArmHpLabel     = config.getString("$ap.armor-health",      apprArmHpLabel)!!
        apprArmDefLabel    = config.getString("$ap.armor-defense",     apprArmDefLabel)!!
        apprArmEvaLabel    = config.getString("$ap.armor-evasion",     apprArmEvaLabel)!!

        val gu = "gui"
        guiAwakeTitle          = config.getString("$gu.awake.title",            guiAwakeTitle)!!
        guiAwakeSlotWeapon     = config.getString("$gu.awake.slot-weapon",      guiAwakeSlotWeapon)!!
        guiAwakeSlotWeaponLore = config.getStringList("$gu.awake.slot-weapon-lore").ifEmpty { guiAwakeSlotWeaponLore }
        guiAwakeSlotScroll     = config.getString("$gu.awake.slot-scroll",      guiAwakeSlotScroll)!!
        guiAwakeSlotScrollLore = config.getStringList("$gu.awake.slot-scroll-lore").ifEmpty { guiAwakeSlotScrollLore }
        guiAwakePending        = config.getString("$gu.awake.pending",          guiAwakePending)!!
        guiAwakeActionOk       = config.getString("$gu.awake.action-ok",        guiAwakeActionOk)!!
        guiAwakeNoMoney        = config.getString("$gu.awake.no-money",         guiAwakeNoMoney)!!
        guiAwakeLabelAction    = config.getString("$gu.awake.label-action",     guiAwakeLabelAction)!!
        guiAwakeLabelCost      = config.getString("$gu.awake.label-cost",       guiAwakeLabelCost)!!
        guiAwakeLabelNoMoney   = config.getString("$gu.awake.label-no-money",   guiAwakeLabelNoMoney)!!
        guiAwakeLabelOk        = config.getString("$gu.awake.label-ok",         guiAwakeLabelOk)!!
        guiAwakeLabelAppraise  = config.getString("$gu.awake.label-appraise",   guiAwakeLabelAppraise)!!
        guiAwakeLabelReappraise= config.getString("$gu.awake.label-reappraise", guiAwakeLabelReappraise)!!
        guiAwakeLabelAwake     = config.getString("$gu.awake.label-awake",      guiAwakeLabelAwake)!!
        guiAwakeLabelReawake   = config.getString("$gu.awake.label-reawake",    guiAwakeLabelReawake)!!
        msgAwakeAppraisalOk    = config.getString("$m.awake-appraisal-ok",      msgAwakeAppraisalOk)!!
        msgAwakeReappraisalOk  = config.getString("$m.awake-reappraisal-ok",    msgAwakeReappraisalOk)!!
        msgAwakeSocketOk       = config.getString("$m.awake-socket-ok",         msgAwakeSocketOk)!!
        msgAwakeSocketRerollOk = config.getString("$m.awake-socket-reroll-ok",  msgAwakeSocketRerollOk)!!
        msgAwakeSlotHint11     = config.getString("$m.awake-slot-hint-11",      msgAwakeSlotHint11)!!
        msgAwakeSlotHint13     = config.getString("$m.awake-slot-hint-13",      msgAwakeSlotHint13)!!
        errAwakeWrongScroll    = config.getString("$m.err-awake-wrong-scroll",  errAwakeWrongScroll)!!
        errAwakeNotRpgItem     = config.getString("$m.err-awake-not-rpg-item",  errAwakeNotRpgItem)!!
        errAwakeApprMaxReached = config.getString("$m.err-awake-appr-max",      errAwakeApprMaxReached)!!
        errAwakeSockMaxReached = config.getString("$m.err-awake-sock-max",      errAwakeSockMaxReached)!!

        guiUpgradeTitle         = config.getString("$gu.upgrade.title",           guiUpgradeTitle)!!
        guiUpgradeSlotItemName  = config.getString("$gu.upgrade.slot-item-name",  guiUpgradeSlotItemName)!!
        guiUpgradeSlotItemLore  = config.getStringList("$gu.upgrade.slot-item-lore").ifEmpty { guiUpgradeSlotItemLore }
        guiUpgradeSlotStoneName = config.getString("$gu.upgrade.slot-stone-name", guiUpgradeSlotStoneName)!!
        guiUpgradeSlotStoneLore = config.getStringList("$gu.upgrade.slot-stone-lore").ifEmpty { guiUpgradeSlotStoneLore }
        guiUpgradeSlotBreakName = config.getString("$gu.upgrade.slot-break-name", guiUpgradeSlotBreakName)!!
        guiUpgradeSlotBreakLore = config.getStringList("$gu.upgrade.slot-break-lore").ifEmpty { guiUpgradeSlotBreakLore }
        guiUpgradeSlotDownName  = config.getString("$gu.upgrade.slot-down-name",  guiUpgradeSlotDownName)!!
        guiUpgradeSlotDownLore  = config.getStringList("$gu.upgrade.slot-down-lore").ifEmpty { guiUpgradeSlotDownLore }
        guiUpgradeSlotResultName= config.getString("$gu.upgrade.slot-result-name",guiUpgradeSlotResultName)!!
        guiUpgradeSlotResultLore= config.getStringList("$gu.upgrade.slot-result-lore").ifEmpty { guiUpgradeSlotResultLore }
        guiUpgradeBtnNoItem     = config.getString("$gu.upgrade.btn-no-item",     guiUpgradeBtnNoItem)!!
        guiUpgradeBtnNoItemLore = config.getStringList("$gu.upgrade.btn-no-item-lore").ifEmpty { guiUpgradeBtnNoItemLore }
        guiUpgradeBtnNotRpg     = config.getString("$gu.upgrade.btn-not-rpg",     guiUpgradeBtnNotRpg)!!
        guiUpgradeBtnNotRpgLore = config.getStringList("$gu.upgrade.btn-not-rpg-lore").ifEmpty { guiUpgradeBtnNotRpgLore }
        guiUpgradeBtnMax        = config.getString("$gu.upgrade.btn-max",          guiUpgradeBtnMax)!!
        guiUpgradeBtnMaxLore    = config.getStringList("$gu.upgrade.btn-max-lore").ifEmpty { guiUpgradeBtnMaxLore }
        guiUpgradeBtnReady      = config.getString("$gu.upgrade.btn-ready",        guiUpgradeBtnReady)!!
        guiUpgradeBtnNotReady   = config.getString("$gu.upgrade.btn-not-ready",    guiUpgradeBtnNotReady)!!
        guiUpgradeBtnLoreStoneOk  = config.getString("$gu.upgrade.btn-stone-ok",  guiUpgradeBtnLoreStoneOk)!!
        guiUpgradeBtnLoreStoneErr = config.getString("$gu.upgrade.btn-stone-err", guiUpgradeBtnLoreStoneErr)!!

        guiRoonTitle         = config.getString("$gu.roon.title",          guiRoonTitle)!!
        guiRoonEmptySlotName = config.getString("$gu.roon.empty-slot-name",guiRoonEmptySlotName)!!
        guiRoonEmptySlotLore = config.getStringList("$gu.roon.empty-slot-lore").ifEmpty { guiRoonEmptySlotLore }

        stoneNameLow  = config.getString("$l.stone-name-low",  stoneNameLow)!!
        stoneNameMid  = config.getString("$l.stone-name-mid",  stoneNameMid)!!
        stoneNameHigh = config.getString("$l.stone-name-high", stoneNameHigh)!!
    }

    // ── 유틸 ──────────────────────────────────────────────────────────
    /** {key} 플레이스홀더를 map 의 값으로 치환 */
    fun format(template: String, vararg pairs: Pair<String, String>): String {
        var result = template
        for ((k, v) in pairs) result = result.replace("{$k}", v)
        return result
    }
}
