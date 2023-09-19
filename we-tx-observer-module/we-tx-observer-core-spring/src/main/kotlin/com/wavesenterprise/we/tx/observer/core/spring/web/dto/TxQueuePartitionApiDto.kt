package com.wavesenterprise.we.tx.observer.core.spring.web.dto

import java.time.OffsetDateTime

data class TxQueuePartitionApiDto(
    val id: String,
    var priority: Int,
    var pausedOnTxId: String? = null,
    var created: OffsetDateTime? = null,
    var modified: OffsetDateTime? = null,
)
