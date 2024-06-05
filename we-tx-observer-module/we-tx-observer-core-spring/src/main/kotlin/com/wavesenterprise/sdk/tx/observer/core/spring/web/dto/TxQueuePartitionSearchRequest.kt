package com.wavesenterprise.sdk.tx.observer.core.spring.web.dto

import com.wavesenterprise.sdk.tx.observer.domain.EnqueuedTxStatus

data class TxQueuePartitionSearchRequest(
    val priority: Int?,
    val priorityOp: PriorityComparisonOperator?,
    val status: List<EnqueuedTxStatus>?,
    val active: Boolean?,
)
