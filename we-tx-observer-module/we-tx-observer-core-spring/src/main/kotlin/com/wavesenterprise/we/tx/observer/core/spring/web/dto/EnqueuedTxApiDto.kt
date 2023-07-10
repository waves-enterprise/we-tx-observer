package com.wavesenterprise.we.tx.observer.core.spring.web.dto

import com.fasterxml.jackson.databind.JsonNode
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus
import java.time.OffsetDateTime

data class EnqueuedTxApiDto(
    val id: String?,
    val body: JsonNode,
    val status: EnqueuedTxStatus,
    val blockHeight: Long,
    val positionInBlock: Int,
    val txTimestamp: OffsetDateTime,
    var available: Boolean,
    var created: OffsetDateTime? = null,
    var modified: OffsetDateTime? = null,
)
