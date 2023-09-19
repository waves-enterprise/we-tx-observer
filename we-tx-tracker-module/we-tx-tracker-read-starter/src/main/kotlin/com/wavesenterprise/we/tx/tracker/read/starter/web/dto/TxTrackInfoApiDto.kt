package com.wavesenterprise.we.tx.tracker.read.starter.web.dto

import com.fasterxml.jackson.databind.JsonNode
import com.wavesenterprise.we.tx.tracker.domain.TxTrackStatus
import java.time.OffsetDateTime
import java.util.UUID

data class TxTrackInfoApiDto(
    val id: String,
    var smartContractId: String?,
    var status: TxTrackStatus,
    var type: Int,
    var body: JsonNode,
    var errors: JsonNode?,
    val meta: Map<String, Any>,
    var created: OffsetDateTime?,
    var modified: OffsetDateTime?,
    val userId: UUID?,
)