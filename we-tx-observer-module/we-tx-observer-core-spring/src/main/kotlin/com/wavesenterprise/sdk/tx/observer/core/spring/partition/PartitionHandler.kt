package com.wavesenterprise.sdk.tx.observer.core.spring.partition

interface PartitionHandler {

    fun handleErrorWhenReading(partitionId: String)

    fun handleSuccessWhenReading(partitionId: String, txCount: Long)

    fun pausePartitionOnTx(partitionId: String, pausedOnTxId: String)

    fun handleEmptyPartition(partitionId: String)

    fun resumePartitionForTx(partitionId: String, txId: String)
}
