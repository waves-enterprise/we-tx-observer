package com.wavesenterprise.we.tx.observer.core.spring.executor

import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class ScheduledTxQueueCleaner(
    val enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
    val syncInfoService: SyncInfoService,
    val archiveBlockHeightWindow: Long,
    val deleteBatchSize: Long,
) {

    val logger: Logger = LoggerFactory.getLogger(ScheduledTxQueueCleaner::class.java)

    @SchedulerLock(
        name = "cleanReadEnqueuedTx_task",
    )
    fun cleanReadEnqueuedTx() {
        val blockHeightLimit = syncInfoService.observerHeight() - archiveBlockHeightWindow
        logger.info("Deleting all READ transactions having blockHeight < $blockHeightLimit")
        var totalDeleted = 0
        do {
            val deletedCount =
                enqueuedTxJpaRepository.deleteAllReadWithBlockHeightBefore(
                    blockHeight = blockHeightLimit,
                    limit = deleteBatchSize
                )
            totalDeleted += deletedCount
        } while (deletedCount > 0)
        logger.info("Finished deleting $totalDeleted transactions")
    }
}
