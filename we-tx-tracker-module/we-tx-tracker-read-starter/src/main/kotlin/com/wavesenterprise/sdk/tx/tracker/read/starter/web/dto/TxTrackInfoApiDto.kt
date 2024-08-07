package com.wavesenterprise.sdk.tx.tracker.read.starter.web.dto

import com.fasterxml.jackson.databind.JsonNode
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackStatus
import java.time.OffsetDateTime

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
    val userId: String?,
)
