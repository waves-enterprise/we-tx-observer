package com.wavesenterprise.we.tx.observer.core.spring.executor.poller

import com.wavesenterprise.we.tx.observer.common.tx.executor.TxExecutor
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.orm.ObjectOptimisticLockingFailureException
import java.lang.Long.min

open class ScheduledBlockInfoSynchronizer(
    private val sourceExecutor: SourceExecutor,
    private val syncInfoService: SyncInfoService,
    private val enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
    private val pauseSyncAtQueueSize: Long,
    private val liquidBlockPollingDelay: Long,
    private val blockHeightWindow: Long,
    private val txExecutor: TxExecutor,
) {

    private val logger: Logger = LoggerFactory.getLogger(ScheduledBlockInfoSynchronizer::class.java)

    @SchedulerLock(
        name = "syncNodeBlockInfo_task",
    )
    open fun syncNodeBlockInfo() {
        if (pauseSyncRequired()) {
            return
        }
        val blockSyncInfo = txExecutor.requiresNew {
            syncInfoService.syncInfo()
        }
        val nodeHeight = blockSyncInfo.nodeHeight
        val observerHeight = blockSyncInfo.observerHeight
        val startHeight = if (nodeHeight == observerHeight) stableNodeHeight(nodeHeight.value) else observerHeight.value
        syncNodeBlockInfo(startHeight, nodeHeight.value)
    }

    private fun pauseSyncRequired(): Boolean =
        enqueuedTxJpaRepository.countByStatus(EnqueuedTxStatus.NEW) >= pauseSyncAtQueueSize

    private fun stableNodeHeight(nodeHeight: Long) =
        nodeHeight - 1

    private fun syncNodeBlockInfo(startHeight: Long, nodeHeight: Long) {
        var syncedToHeight = startHeight
        while (syncedToHeight <= nodeHeight) {
            try {
                syncedToHeight = sync(
                    syncedToHeight,
                    nodeHeight
                )
                logger.debug("Synced to height $syncedToHeight")
            } catch (ex: ObjectOptimisticLockingFailureException) {
                logger.debug(
                    "Sync failed [startHeight = '$startHeight', " +
                        "nodeHeight = '$nodeHeight']",
                    ex
                )
            }
        }
    }

    private fun sync(observerHeight: Long, nodeHeight: Long): Long {
        val syncToHeight = min(
            observerHeight + blockHeightWindow,
            nodeHeight + OFFSET
        )
        logger.debug("Syncing to height $syncToHeight")
        val newHeight = txExecutor.requiresNew {
            sourceExecutor.execute(
                blockHeightLowerBound = observerHeight,
                blockHeightUpperBound = syncToHeight
            ).also { newHeight ->
                syncInfoService.syncedTo(newHeight)
            }
        }
        if (newHeight == observerHeight) {
            runBlocking { delay(liquidBlockPollingDelay) }
        }
        return newHeight
    }

    companion object {
        const val OFFSET = 1
    }
}
