package io.zlero.cRRPGCore

import io.zlero.cRFramework.CRPlugin
import net.milkbowl.vault.economy.Economy

class CRRPGCorePlugin : CRPlugin() {

    companion object {
        lateinit var plugin: CRRPGCorePlugin
            private set
    }

    lateinit var msgCfg:             MessageConfig
    lateinit var levelManager:       LevelManager
    lateinit var statManager:        StatManager
    lateinit var playerDataManager:  PlayerDataManager
    lateinit var rpgItemManager:     RpgItemManager
    lateinit var upgradeManager:     UpgradeManager
    lateinit var jewelManager:       JewelManager
    lateinit var armorHealthManager: ArmorHealthManager
    lateinit var actionBarManager:   ActionBarManager
    lateinit var appraisalManager:   AppraisalManager
    lateinit var socketManager:      SocketManager
    lateinit var xpBoostManager:     XpBoostManager

    var economy: Economy? = null

    override fun components() = listOf(
        MessageConfig::class,
        PlayerDataManager::class,
        LevelManager::class,
        StatManager::class,
        RpgItemManager::class,
        UpgradeManager::class,
        JewelManager::class,
        ArmorHealthManager::class,
        ActionBarManager::class,
        AppraisalManager::class,
        SocketManager::class,
        XpBoostManager::class,
        // Listeners (@Subscribe 자동 등록)
        LevelListener::class,
        StatListener::class,
        RpgItemListener::class,
        RpgDurabilityListener::class,
        PlayerSessionListener::class,
        // Commands
        RpgCoreCommand::class,
        StatCommand::class,
        RerollSettingCommand::class,
    )

    override fun onCREnabled() {
        plugin = this

        saveDefaultConfig()
        val cfg = config

        val rsp = server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            logger.warning("[CRRPGCore] Vault Economy를 찾을 수 없습니다. 비용 기능이 비활성화됩니다.")
        } else {
            economy = rsp.provider
            logger.info("[CRRPGCore] Vault Economy 연동 완료: ${economy!!.name}")
        }

        msgCfg            = inject<MessageConfig>().also            { it.load(cfg) }
        playerDataManager = inject<PlayerDataManager>().also        { it.load() }
        levelManager      = inject<LevelManager>().also             { it.loadConfig(cfg) }
        statManager       = inject<StatManager>().also              { it.loadConfig() }
        rpgItemManager    = inject<RpgItemManager>().also           { it.loadConfig(cfg) }
        upgradeManager    = inject<UpgradeManager>().also           { it.loadConfig(cfg) }
        jewelManager      = inject<JewelManager>()
        armorHealthManager= inject<ArmorHealthManager>()
        actionBarManager  = inject<ActionBarManager>()
        appraisalManager  = inject<AppraisalManager>().also         { it.loadConfig(cfg) }
        socketManager     = inject<SocketManager>().also            { it.loadConfig(cfg) }
        xpBoostManager    = inject<XpBoostManager>()
        XpBoostScroll.init(this)

        // object 기반 리스너는 DI 불가 → 직접 등록 유지
        val pm = server.pluginManager
        pm.registerEvents(StatGui,          this)
        pm.registerEvents(RoonGui,          this)
        pm.registerEvents(UpgradeGui,       this)
        pm.registerEvents(LevelResetScroll, this)
        pm.registerEvents(StatResetScroll,  this)
        pm.registerEvents(AwakeGui,         this)
        pm.registerEvents(XpBoostScroll,    this)

        // 커맨드 등록 (탭 완성 지원을 위해 수동 등록 유지)
        getCommand("rpgcore")?.let {
            val cmd = inject<RpgCoreCommand>()
            it.setExecutor(cmd)
            it.tabCompleter = cmd
        }
        getCommand("스텟")?.let {
            val cmd = inject<StatCommand>()
            it.setExecutor(cmd)
            it.tabCompleter = cmd
        }
        getCommand("리롤설정")?.setExecutor(inject<RerollSettingCommand>())

        actionBarManager.start()

        // 만료된 XP 부스트 엔트리 주기적 정리 (5분마다)
        scheduler.runTimerAsync(6000L, 6000L) {
            xpBoostManager.pruneExpired()
        }

        logger.info("[CRRPGCore] v${description.version} 활성화 완료!")
    }

    override fun onCRDisabled() {
        UpgradeGui.closeAll()
        actionBarManager.stop()
        server.onlinePlayers.forEach { player ->
            val data = levelManager.getPlayerData(player)
            playerDataManager.savePlayer(player.uniqueId, data)
            jewelManager.saveSlots(player)
        }
        playerDataManager.close()
        logger.info("[CRRPGCore] 비활성화.")
    }

    fun reloadAll() {
        reloadConfig()
        val cfg = config
        msgCfg.load(cfg)
        levelManager.loadConfig(cfg)
        statManager.loadConfig()
        rpgItemManager.loadConfig(cfg)
        upgradeManager.loadConfig(cfg)
        appraisalManager.loadConfig(cfg)
        socketManager.loadConfig(cfg)
    }
}
