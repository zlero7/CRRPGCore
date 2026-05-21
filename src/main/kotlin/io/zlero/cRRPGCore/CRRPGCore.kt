package io.zlero.cRRPGCore

import io.zlero.cRFramework.CRPlugin
import net.milkbowl.vault.economy.Economy

class CRRPGCorePlugin : CRPlugin() {

    companion object {
        lateinit var plugin: CRRPGCorePlugin
            private set
    }

    lateinit var msgCfg:              MessageConfig
    lateinit var levelManager:        LevelManager
    lateinit var statManager:         StatManager
    lateinit var playerDataRepository: PlayerDataRepository
    lateinit var roonSlotRepository:  RoonSlotRepository
    lateinit var rpgItemManager:      RpgItemManager
    lateinit var upgradeManager:      UpgradeManager
    lateinit var jewelManager:        JewelManager
    lateinit var armorHealthManager:  ArmorHealthManager
    lateinit var actionBarManager:    ActionBarManager
    lateinit var appraisalManager:    AppraisalManager
    lateinit var socketManager:       SocketManager
    lateinit var xpBoostManager:      XpBoostManager
    lateinit var migrationManager:    MigrationManager

    var economy: Economy? = null

    // config.yml 을 onLoad() 에서 미리 생성해 DI scan 시점에 plugin.config 접근 가능하게 함
    override fun onLoad() {
        super.onLoad()
        saveDefaultConfig()
    }

    override fun components() = listOf(
        // CRFramework DB 모듈 — config.yml storage.type 에 따라 SQLite / MySQL 자동 선택
        CRRPGDatabaseModule::class,
        PlayerDataRepository::class,
        RoonSlotRepository::class,
        // 매니저
        MessageConfig::class,
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
        // Commands
        RpgCoreCommand::class,
        StatCommand::class,
        RerollSettingCommand::class,
    )

    override fun onCREnabled() {
        plugin = this

        saveDefaultConfig()
        val cfg = config

        // Vault Economy 연동
        val rsp = server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            logger.warning("[CRRPGCore] Vault Economy를 찾을 수 없습니다. 비용 기능이 비활성화됩니다.")
        } else {
            economy = rsp.provider
            logger.info("[CRRPGCore] Vault Economy 연동 완료: ${economy!!.name}")
        }

        // DB 연결 + 테이블 생성은 CRRPGDatabaseModule.@Setup 에서 이미 완료
        // PlayerStorageListener 수동 등록 → PlayerRepository onJoin/onQuit 트리거
        playerDataRepository = inject<PlayerDataRepository>()
        roonSlotRepository   = inject<RoonSlotRepository>()
        server.pluginManager.registerEvents(
            PlayerStorageListener(playerDataRepository, roonSlotRepository), this
        )

        // 매니저 초기화
        msgCfg            = inject<MessageConfig>().also            { it.load(cfg) }
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
        migrationManager  = MigrationManager(this)
        XpBoostScroll.init(this)

        // object 기반 리스너 직접 등록
        // StatGui → StatView(CRFramework View) 로 대체되어 pm.registerEvents 불필요
        val pm = server.pluginManager
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
        // PlayerRepository dirty 데이터 자동 저장 + DB 연결 종료는
        // CRRPGDatabaseModule.@Teardown → registry.teardown() 에서 처리
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
