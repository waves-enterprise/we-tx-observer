package com.wavesenterprise.sdk.tx.observer.core.spring.web.dto

import java.time.OffsetDateTime

data class RollbackInfoApiDto(
    val toHeight: Long,
    val toBlockSignature: String,
    val datetime: OffsetDateTime,
)
