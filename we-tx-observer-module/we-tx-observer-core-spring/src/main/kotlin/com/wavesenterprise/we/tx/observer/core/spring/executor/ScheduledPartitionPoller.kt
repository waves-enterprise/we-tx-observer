package com.wavesenterprise.we.tx.observer.core.spring.executor

import com.wavesenterprise.we.tx.observer.core.spring.partition.LatestTxPartitionPoller

open class ScheduledPartitionPoller(
    private val errorHandlingLatestTxPartitionPoller: LatestTxPartitionPoller,
) {

    open fun pollWhileHavingActivePartitions() {
        while (true) {
            errorHandlingLatestTxPartitionPoller.pollLatestActualPartition() ?: break
        }
    }
}
