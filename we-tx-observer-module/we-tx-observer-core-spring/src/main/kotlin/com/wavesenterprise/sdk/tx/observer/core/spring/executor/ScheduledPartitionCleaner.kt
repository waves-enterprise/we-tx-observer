package com.wavesenterprise.sdk.tx.observer.core.spring.executor

import com.wavesenterprise.sdk.tx.observer.core.spring.properties.PartitionCleanerConfig
import com.wavesenterprise.sdk.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class ScheduledPartitionCleaner(
    val txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository,
    val partitionCleanerConfig: PartitionCleanerConfig,
) {
    val logger: Logger = LoggerFactory.getLogger(ScheduledPartitionCleaner::class.java)

    @SchedulerLock(
        name = "cleanEmptyPartitions_task",
    )
    open fun cleanEmptyPartitions() {
        logger.info("Cleaning empty partitions...")
        var totalDeletedCount = 0
        do {
            val deletedCount = txQueuePartitionJpaRepository.deleteEmptyPartitions(
                limit = partitionCleanerConfig.batchSize,
            )
            totalDeletedCount += deletedCount
        } while (deletedCount > 0)
        logger.info("Deleted $totalDeletedCount empty partitions")
    }
}
