package com.wavesenterprise.we.tx.observer.core.spring.web.dto

import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus

data class EnqueuedTxSearchRequest(
    var status: List<EnqueuedTxStatus>?,
    var blockHeight: Long?,
    var partitionId: String?,
    var txType: List<Int>?,
    var available: Boolean?,
)
