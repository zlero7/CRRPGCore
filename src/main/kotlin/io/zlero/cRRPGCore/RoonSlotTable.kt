package io.zlero.cRRPGCore

import org.jetbrains.exposed.dao.id.IntIdTable

/**
 * 플레이어 룬(보석) 슬롯 테이블
 * 9개 슬롯을 Base64 문자열로 직렬화하여 단일 text 컬럼에 저장 ("|" 구분자)
 */
object RoonSlotTable : IntIdTable("crrpg_roon") {
    val uuid  = varchar("uuid", 36).uniqueIndex()
    val slots = text("slots").default("")
}
