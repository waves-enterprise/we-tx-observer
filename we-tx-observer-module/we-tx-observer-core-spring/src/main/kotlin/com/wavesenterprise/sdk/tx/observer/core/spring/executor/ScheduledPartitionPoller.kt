package com.wavesenterprise.sdk.tx.observer.core.spring.executor

import com.wavesenterprise.sdk.tx.observer.core.spring.partition.TxPartitionPoller

open class ScheduledPartitionPoller(
    private val errorHandlingTxPartitionPoller: TxPartitionPoller,
) {

    open fun pollWhileHavingActivePartitions() {
        while (true) {
            errorHandlingTxPartitionPoller.pollPartition() ?: break
        }
    }
}
