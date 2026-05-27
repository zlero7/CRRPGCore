package io.zlero.cRRPGCore.manager

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.LinkedList

class UpgradeHistoryManager {
    data class HistoryEntry(
        val time:      String,
        val itemName:  String,
        val outcome:   UpgradeManager.UpgradeOutcome,
        val prevLevel: Int,
        val newLevel:  Int
    )

    private val history = ConcurrentHashMap<UUID, LinkedList<HistoryEntry>>()
    private val fmt = DateTimeFormatter.ofPattern("MM/dd HH:mm")

    fun record(uuid: UUID, itemName: String, outcome: UpgradeManager.UpgradeOutcome, prev: Int, new: Int) {
        val list = history.getOrPut(uuid) { LinkedList() }
        list.addFirst(HistoryEntry(LocalDateTime.now().format(fmt), itemName, outcome, prev, new))
        if (list.size > 20) list.removeLast()
    }

    fun getHistory(uuid: UUID): List<HistoryEntry> = history[uuid] ?: emptyList()

    fun clear(uuid: UUID) { history.remove(uuid) }
}
