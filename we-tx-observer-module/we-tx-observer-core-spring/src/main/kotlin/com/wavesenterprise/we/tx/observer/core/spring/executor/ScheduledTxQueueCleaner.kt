package com.wavesenterprise.we.tx.observer.core.spring.executor

import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.we.tx.observer.core.spring.properties.QueueCleanerConfig
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class ScheduledTxQueueCleaner(
    val enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
    val syncInfoService: SyncInfoService,
    val queueCleanerConfig: QueueCleanerConfig,
) {

    val logger: Logger = LoggerFactory.getLogger(ScheduledTxQueueCleaner::class.java)

    @SchedulerLock(
        name = "cleanReadEnqueuedTx_task",
        lockAtLeastFor = "\${tx-observer.queue-cleaner.lock-at-least:PT0S}",
        lockAtMostFor = "\${tx-observer.queue-cleaner.lock-at-most:PT1M}",
    )
    open fun cleanReadEnqueuedTx() {
        val blockHeightLimit = syncInfoService.observerHeight() - queueCleanerConfig.archiveHeightWindow
        logger.info("Deleting all READ transactions having blockHeight < $blockHeightLimit")
        var totalDeleted = 0
        do {
            val deletedCount =
                enqueuedTxJpaRepository.deleteAllReadWithBlockHeightBefore(
                    blockHeight = blockHeightLimit,
                    limit = queueCleanerConfig.deleteBatchSize
                )
            totalDeleted += deletedCount
        } while (deletedCount > 0)
        logger.info("Finished deleting $totalDeleted transactions")
    }
}
