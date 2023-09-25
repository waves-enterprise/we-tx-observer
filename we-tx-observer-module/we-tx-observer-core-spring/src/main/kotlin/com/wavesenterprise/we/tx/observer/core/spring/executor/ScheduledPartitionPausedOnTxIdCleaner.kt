package com.wavesenterprise.we.tx.observer.core.spring.executor

import com.wavesenterprise.we.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class ScheduledPartitionPausedOnTxIdCleaner(
    val txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository,
) {

    val logger: Logger = LoggerFactory.getLogger(ScheduledPartitionPausedOnTxIdCleaner::class.java)

    open fun clear() {
        val count = txQueuePartitionJpaRepository.clearPausedOnTxIds()
        logger.info("pausedOnTxId has been set to null for $count partitions.")
    }
}
