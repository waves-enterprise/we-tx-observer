package com.wavesenterprise.we.tx.observer.core.spring.partition

import com.wavesenterprise.we.tx.observer.api.PartitionHandlingException

class ErrorHandlingLatestTxPartitionPoller(
    private val defaultLatestTxPartitionPoller: LatestTxPartitionPoller,
    private val partitionHandler: PartitionHandler,
) : LatestTxPartitionPoller {

    override fun pollLatestActualPartition(): String? = try {
        defaultLatestTxPartitionPoller.pollLatestActualPartition()
    } catch (ex: PartitionHandlingException) {
        partitionHandler.handleErrorWhenReading(ex.partitionId)
        ex.partitionId
    }
}
