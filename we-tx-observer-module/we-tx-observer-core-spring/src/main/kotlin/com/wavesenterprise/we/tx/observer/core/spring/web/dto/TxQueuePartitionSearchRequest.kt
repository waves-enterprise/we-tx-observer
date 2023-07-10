package com.wavesenterprise.we.tx.observer.core.spring.web.dto

import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus

data class TxQueuePartitionSearchRequest(
    val priority: Int?,
    val priorityOp: PriorityComparisonOperator?,
    val status: List<EnqueuedTxStatus>?,
    val active: Boolean?,
)
