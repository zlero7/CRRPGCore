package io.zlero.cRRPGCore

import org.jetbrains.exposed.dao.id.IntIdTable

/**
 * 플레이어 레벨/스텟 테이블
 * CRFramework DatabaseModule (Exposed ORM) 사용
 */
object PlayerDataTable : IntIdTable("crrpg_players") {
    val uuid       = varchar("uuid", 36).uniqueIndex()
    val level      = integer("level").default(1)
    val xp         = long("xp").default(0L)
    val statPoints = integer("stat_points").default(0)
    val strength   = integer("strength").default(0)
    val vitality   = integer("vitality").default(0)
    val agility    = integer("agility").default(0)
}
