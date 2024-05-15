package com.wavesenterprise.we.tx.observer.core.spring.executor

import com.wavesenterprise.we.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

open class ScheduledPartitionCleaner(
    val txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository,
) {
    val logger: Logger = LoggerFactory.getLogger(ScheduledPartitionCleaner::class.java)

    @Transactional
    open fun clean() {
        val deletedCount = txQueuePartitionJpaRepository.deleteEmptyPartitions()
        logger.info("Deleted $deletedCount empty partitions")
    }
}
