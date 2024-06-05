package com.wavesenterprise.sdk.tx.observer.core.spring.executor

import com.wavesenterprise.sdk.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

open class ScheduledPartitionPausedOnTxIdCleaner(
    val txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository,
) {

    val logger: Logger = LoggerFactory.getLogger(ScheduledPartitionPausedOnTxIdCleaner::class.java)

    @Transactional
    open fun clear() {
        val count = txQueuePartitionJpaRepository.clearPausedOnTxIds()
        logger.info("pausedOnTxId has been set to null for $count partitions.")
    }
}
