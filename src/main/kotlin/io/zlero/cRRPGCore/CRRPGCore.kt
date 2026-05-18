package io.zlero.cRRPGCore

import net.milkbowl.vault.economy.Economy
import org.bukkit.plugin.java.JavaPlugin

class CRRPGCorePlugin : JavaPlugin() {

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

    override fun onEnable() {
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

        msgCfg            = MessageConfig(this).also { it.load(cfg) }
        playerDataManager = PlayerDataManager(this).also { it.load() }
        levelManager      = LevelManager(this).also      { it.loadConfig(cfg) }
        statManager       = StatManager(this).also       { it.loadConfig() }
        rpgItemManager    = RpgItemManager(this).also    { it.loadConfig(cfg) }
        upgradeManager    = UpgradeManager(this).also    { it.loadConfig(cfg) }
        jewelManager      = JewelManager(this)
        armorHealthManager= ArmorHealthManager(this)
        actionBarManager  = ActionBarManager(this)
        appraisalManager  = AppraisalManager(this).also { it.loadConfig(cfg) }
        socketManager     = SocketManager(this).also    { it.loadConfig(cfg) }
        xpBoostManager    = XpBoostManager()
        XpBoostScroll.init(this)

        val pm = server.pluginManager
        pm.registerEvents(LevelListener(this, levelManager), this)
        pm.registerEvents(StatListener(this),           this)
        pm.registerEvents(StatGui,                      this)
        pm.registerEvents(RoonGui,                      this)
        pm.registerEvents(UpgradeGui,                   this)
        pm.registerEvents(armorHealthManager,           this)
        pm.registerEvents(RpgItemListener(this),        this)
        pm.registerEvents(RpgDurabilityListener(this),  this)
        pm.registerEvents(LevelResetScroll,             this)
        pm.registerEvents(StatResetScroll,              this)
        pm.registerEvents(AwakeGui,                     this)
        pm.registerEvents(PlayerSessionListener(this),  this)
        pm.registerEvents(XpBoostScroll,                this)

        getCommand("rpgcore")?.let {
            val cmd = RpgCoreCommand(this)
            it.setExecutor(cmd)
            it.tabCompleter = cmd
        }
        getCommand("스텟")?.let {
            val cmd = StatCommand(this)
            it.setExecutor(cmd)
            it.tabCompleter = cmd
        }
        getCommand("리롤설정")?.setExecutor(RerollSettingCommand(this))

        actionBarManager.start()

        // 만료된 XP 부스트 엔트리 주기적 정리 (5분마다)
        server.scheduler.runTaskTimerAsynchronously(this, Runnable {
            xpBoostManager.pruneExpired()
        }, 6000L, 6000L)

        logger.info("[CRRPGCore] v${description.version} 활성화 완료!")
    }

    override fun onDisable() {
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
