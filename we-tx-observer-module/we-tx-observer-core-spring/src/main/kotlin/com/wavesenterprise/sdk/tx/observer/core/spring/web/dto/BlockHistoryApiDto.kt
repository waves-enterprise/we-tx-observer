package com.wavesenterprise.sdk.tx.observer.core.spring.web.dto

import java.time.OffsetDateTime

data class BlockHistoryApiDto(
    val signature: String,
    val height: Long,
    val timestamp: OffsetDateTime,
    val createdTimestamp: OffsetDateTime,
)
