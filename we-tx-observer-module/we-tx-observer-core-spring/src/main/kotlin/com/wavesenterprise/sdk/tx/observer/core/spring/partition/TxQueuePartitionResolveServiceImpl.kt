package com.wavesenterprise.sdk.tx.observer.core.spring.partition

import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.tx.observer.api.partition.TxQueuePartitionResolver
import com.wavesenterprise.sdk.tx.observer.domain.TxQueuePartition
import com.wavesenterprise.sdk.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class TxQueuePartitionResolveServiceImpl(
    val txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository,
    val partitionResolver: TxQueuePartitionResolver,
    val defaultPartitionId: String,
) : TxQueuePartitionResolveService {

    private val logger: Logger = LoggerFactory.getLogger(TxQueuePartitionResolveService::class.java)

    override fun resolveTxQueuePartition(tx: Tx, resetLastReadTxId: Boolean): TxQueuePartition =
        (partitionResolver.resolvePartitionId(tx) ?: defaultPartitionId).let { partitionId ->
            txQueuePartitionJpaRepository.findAndLockById(partitionId)
                ?: txQueuePartitionJpaRepository.save(
                    TxQueuePartition(
                        id = partitionId,
                        priority = 0,
                    )
                )
        }.also {
            logger.debug("Resolved partitionId = ${it.id} for TX with ID = ${tx.id}")
        }
}
