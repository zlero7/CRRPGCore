package io.zlero.cRRPGCore

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * 플레이어 접속/퇴장 시 PlayerRepository 생명주기 트리거
 *
 * CRFramework DatabaseModule 의 내부 PlayerLifecycleListener 를 대체합니다.
 * CRRPGDatabaseModule(@Module) 은 final 제약으로 DatabaseModule 을 상속할 수 없어,
 * addPlayerRepository() 대신 이 리스너를 수동 등록하는 방식으로 동일한 역할을 수행합니다.
 *
 * 이벤트 우선순위:
 *   - JOIN  NORMAL  : DB에서 데이터 로드 (LevelListener HIGH 보다 먼저 실행)
 *   - QUIT  MONITOR : 모든 처리 완료 후 마지막에 dirty 데이터 저장
 */
class PlayerStorageListener(
    private val playerDataRepo : PlayerDataRepository,
    private val roonSlotRepo   : RoonSlotRepository
) : Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onJoin(event: PlayerJoinEvent) {
        val uuid = event.player.uniqueId
        playerDataRepo.onJoin(uuid)
        roonSlotRepo.onJoin(uuid)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        playerDataRepo.onQuit(uuid)
        roonSlotRepo.onQuit(uuid)
    }
}
