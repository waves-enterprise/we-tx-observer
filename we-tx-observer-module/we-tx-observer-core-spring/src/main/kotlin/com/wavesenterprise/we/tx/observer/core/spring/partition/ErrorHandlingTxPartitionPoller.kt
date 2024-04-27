package com.wavesenterprise.we.tx.observer.core.spring.partition

import com.wavesenterprise.we.tx.observer.api.PartitionHandlingException

class ErrorHandlingTxPartitionPoller(
    private val defaultTxPartitionPoller: TxPartitionPoller,
    private val partitionHandler: PartitionHandler,
) : TxPartitionPoller {

    override fun pollPartition(): String? = try {
        defaultTxPartitionPoller.pollPartition()
    } catch (ex: PartitionHandlingException) {
        partitionHandler.handleErrorWhenReading(ex.partitionId)
        ex.partitionId
    }
}
