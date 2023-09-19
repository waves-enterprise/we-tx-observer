package com.wavesenterprise.we.tx.observer.core.spring.executor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.wavesenterprise.sdk.node.client.feign.tx.mapDto
import com.wavesenterprise.sdk.node.domain.Timestamp.Companion.toDateTimeFromUTCBlockChain
import com.wavesenterprise.sdk.node.domain.TxType
import com.wavesenterprise.sdk.node.domain.tx.AtomicTx
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.domain.tx.Tx.Companion.type
import com.wavesenterprise.we.tx.observer.api.block.WeBlockInfo
import com.wavesenterprise.we.tx.observer.api.block.subscriber.BlockSubscriber
import com.wavesenterprise.we.tx.observer.api.tx.TxEnqueuePredicate
import com.wavesenterprise.we.tx.observer.core.spring.metrics.AddableLongMetricsContainer
import com.wavesenterprise.we.tx.observer.core.spring.partition.TxQueuePartitionResolveService
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTx
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository

class EnqueueingBlockSubscriber(
    val enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
    val txQueuePartitionResolveService: TxQueuePartitionResolveService,
    val txEnqueuePredicate: TxEnqueuePredicate,
    val totalTxMetricsContainer: AddableLongMetricsContainer,
    val filteredTxMetricContainer: AddableLongMetricsContainer,
    val totalLogicalTxMetricsContainer: AddableLongMetricsContainer,
    val objectMapper: ObjectMapper = jacksonObjectMapper()
) : BlockSubscriber {

    override fun subscribe(weBlockInfo: WeBlockInfo) {
        if (weBlockInfo.txCount == 0L) return
        totalTxMetricsContainer.add(weBlockInfo.txCount)
        weBlockInfo.txList.withIndex()
            .flatMap { (positionInBlock, txInfo) ->
                if (txInfo.tx is AtomicTx) {
                    mutableListOf(
                        TxWithPositionInfo(
                            positionInBlock = positionInBlock,
                            positionInAtomic = 0,
                            tx = txInfo.tx,
                            atomicTx = null
                        )
                    ) + (txInfo.tx as AtomicTx).txs.withIndex().map { (positionInAtomic, txInAtomic) ->
                        TxWithPositionInfo(
                            positionInBlock = positionInBlock,
                            positionInAtomic = positionInAtomic + 1,
                            tx = txInAtomic,
                            atomicTx = txInfo.tx as AtomicTx
                        )
                    }
                } else {
                    listOf(
                        TxWithPositionInfo(
                            positionInBlock = positionInBlock,
                            tx = txInfo.tx,
                            atomicTx = null
                        )
                    )
                }
            }.also { logicalTxs ->
                totalLogicalTxMetricsContainer.add(logicalTxs.size.toLong())
            }.run {
                val existentTxIds = enqueuedTxJpaRepository.existentTxIds(map { it.tx.id.asBase58String() }.toSet())
                filterNot { existentTxIds.contains(it.tx.id.asBase58String()) }
            }.filter {
                txEnqueuePredicate.isEnqueued(it.tx)
            }.also { filteredTx ->
                filteredTxMetricContainer.add(filteredTx.size.toLong())
            }.map {
                it.toEnqueuedTx(weBlockInfo.height.value)
            }.also {
                enqueuedTxJpaRepository.saveAll(it)
            }
    }

    private data class TxWithPositionInfo(
        val positionInBlock: Int,
        val positionInAtomic: Int? = null,
        val tx: Tx,
        val atomicTx: AtomicTx?
    )

    private fun TxWithPositionInfo.toEnqueuedTx(
        blockHeight: Long
    ): EnqueuedTx = EnqueuedTx(
        partition = txQueuePartitionResolveService.resolveTxQueuePartition(tx),
        id = tx.id.asBase58String(),
        body = objectMapper.valueToTree(mapDto(tx)),
        status = EnqueuedTxStatus.NEW,
        positionInBlock = positionInBlock,
        positionInAtomic = positionInAtomic,
        atomicTxId = atomicTx?.id?.asBase58String(),
        blockHeight = blockHeight,
        txTimestamp = tx.timestamp.toDateTimeFromUTCBlockChain(),
        txType = tx.type().code,
        available = tx.type().code != TxType.POLICY_DATA_HASH.code
    )
}
