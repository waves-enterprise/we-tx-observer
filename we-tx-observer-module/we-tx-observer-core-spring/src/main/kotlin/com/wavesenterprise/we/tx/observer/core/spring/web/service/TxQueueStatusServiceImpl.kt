package com.wavesenterprise.we.tx.observer.core.spring.web.service

import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.node.client.blocking.tx.TxService
import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.tx.TxInfo
import com.wavesenterprise.we.tx.observer.api.block.WeBlockInfo
import com.wavesenterprise.we.tx.observer.api.block.subscriber.BlockSubscriber
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.we.tx.observer.core.spring.web.dto.PatchTxApiDto
import com.wavesenterprise.we.tx.observer.core.spring.web.dto.PrivacyStatusApiDto
import com.wavesenterprise.we.tx.observer.core.spring.web.dto.QueueStatusApiDto
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTx
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

open class TxQueueStatusServiceImpl(
    val nodeBlockingServiceFactory: NodeBlockingServiceFactory,
    private val syncInfoService: SyncInfoService,
    val enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
    val enqueueingBlockSubscriber: BlockSubscriber,
    val errorPriorityOffset: Int,
) : TxQueueService {

    private val txService: TxService = nodeBlockingServiceFactory.txService()
    private val blocksService: BlocksService = nodeBlockingServiceFactory.blocksService()

    private val log: Logger = LoggerFactory.getLogger(TxQueueStatusServiceImpl::class.java)

    override fun getQueueStatus(): QueueStatusApiDto {
        val nodeHeight = blocksService.blockHeight()
        val observerHeight = syncInfoService.observerHeight()
        val queueHeight = enqueuedTxJpaRepository.findMinHeightForStatus(EnqueuedTxStatus.NEW)
        val queueSize = enqueuedTxJpaRepository.countByStatus(EnqueuedTxStatus.NEW)
        return QueueStatusApiDto(
            nodeHeight = nodeHeight.value,
            observerHeight = observerHeight,
            queueHeight = queueHeight,
            queueSize = queueSize,
            privacyStatusApiDto = PrivacyStatusApiDto(
                totalNewPolicyDataHashes = enqueuedTxJpaRepository.countPolicyDataHashes(),
                notAvailableCount = enqueuedTxJpaRepository.countNotAvailablePolicyDataHashes()
            )
        )
    }

    @Transactional
    override fun deleteTxFromQueue(txId: String) {
        enqueuedTxJpaRepository.deleteById(txId)
    }

    @Transactional
    override fun deleteForked() =
        enqueuedTxJpaRepository.deleteByStatus(enqueuedTxStatus = EnqueuedTxStatus.CANCELLED_FORKED)

    override fun getTxById(txId: TxId): EnqueuedTx = enqueuedTxJpaRepository.findByIdOrNull(txId.asBase58String())
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "TX with ID = $txId not found in tx-observer queue")

    @Transactional
    override fun addTxToQueueById(txId: TxId): EnqueuedTx {
        if (enqueuedTxJpaRepository.existsById(txId.asBase58String())) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "TX with ID = $txId already exists in queue")
        }
        val transactionInfo = txService.txInfo(txId).orElseGet { null }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "TX with ID = $txId not found in node")
        enqueueingBlockSubscriber.subscribe(
            object : WeBlockInfo {
                override val height: Height =
                    requireNotNull(transactionInfo.height) { "Height is empty for TX with ID = $txId" }
                override val txList: List<TxInfo> = listOf(transactionInfo)
                override val signature: Signature = Signature("fake_signature_for_put".toByteArray())
                override val txCount: Long = 1
            }
        )

        return requireNotNull(enqueuedTxJpaRepository.findByIdOrNull(txId.asBase58String())) {
            "Couldn't find tx that was just put. TX ID = $txId"
        }.also {
            log.info("Enqueued tx with ID = $txId")
        }
    }

    @Transactional
    override fun changeTxStatusInQueue(txId: String, patchTxDto: PatchTxApiDto): EnqueuedTx {
        val enqueuedTx = enqueuedTxJpaRepository.findByIdOrNull(txId)
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "TX with ID = $txId not found in tx-observer queue"
            )
        return enqueuedTxJpaRepository.save(
            enqueuedTx.apply {
                status = patchTxDto.status
            }
        )
    }

    @Transactional
    override fun resetToHeightAndReturnDeletedTxCount(blockHeight: Long): Int {
        syncInfoService.resetTo(blockHeight)
        return enqueuedTxJpaRepository.deleteAllWithBlockHeightMoreThan(blockHeight)
    }

    @Transactional
    override fun postponeErrors(): Int {
        return enqueuedTxJpaRepository.setStatusForTxWithStatusEqualsAndPartitionPriorityLowerThan(
            EnqueuedTxStatus.POSTPONED, EnqueuedTxStatus.NEW, -errorPriorityOffset
        )
    }
}
