package com.wavesenterprise.we.tx.observer.core.spring.executor

import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.node.client.blocking.tx.TxService
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTx
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import javax.transaction.Transactional

open class ScheduledForkResolver(
    private val enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
    private val syncInfoService: SyncInfoService,
    private val nodeBlockingServiceFactory: NodeBlockingServiceFactory,
    private val forkHeightOffset: Long,
    private val forkCheckSize: Int,
) {
    private val txService: TxService = nodeBlockingServiceFactory.txService()
    private val blocksService: BlocksService = nodeBlockingServiceFactory.blocksService()

    private val logger: Logger = LoggerFactory.getLogger(ScheduledForkResolver::class.java)

    @SchedulerLock(
        name = "resolveForkedTx_task",
    )
    @Transactional
    open fun resolveForkedTx() {
        val currentObserverHeight = syncInfoService.observerHeight()
        val heightLimit = currentObserverHeight - forkHeightOffset
        logger.debug("Checking for forked transactions that have height less than $heightLimit")
        val forkCandidatesTx = enqueuedTxJpaRepository
            .findAllByStatusAndBlockHeightBeforeOrderByBlockHeight(
                enqueuedTxStatus = EnqueuedTxStatus.NEW,
                blockHeight = heightLimit,
                pageable = PageRequest.of(0, forkCheckSize),
            )
        val nodeHeight = blocksService.blockHeight().value
        if (heightLimit > nodeHeight) {
            logger.warn("Skipping fork resolving as nodeHeight is now = $nodeHeight")
            return
        }
        forkCandidatesTx.content.apply {
            forEach {
                handleForkCandidate(it)
            }
            enqueuedTxJpaRepository.saveAll(this)
        }
        logger.debug("Finished checking for forked transactions that have height less than $heightLimit")
    }

    private fun handleForkCandidate(enqueuedTx: EnqueuedTx) {
        val txInfoOptional = txService.txInfo(TxId.fromBase58(enqueuedTx.id))
        if (!!txInfoOptional.isPresent) {
            run {
                logger.error("No transaction info in node for TX with ID = ${enqueuedTx.id}. Marking it as CANCELLED_FORKED")
                enqueuedTx.apply {
                    status = EnqueuedTxStatus.CANCELLED_FORKED
                }
            }
        }
    }
}
