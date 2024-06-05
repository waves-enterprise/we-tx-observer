package com.wavesenterprise.sdk.tx.observer.core.spring.web.dto

data class TxQueuePartitionStatusApiDto(
    val errorPartitionCount: Long,
    val totalPartitionCount: Long,
)
