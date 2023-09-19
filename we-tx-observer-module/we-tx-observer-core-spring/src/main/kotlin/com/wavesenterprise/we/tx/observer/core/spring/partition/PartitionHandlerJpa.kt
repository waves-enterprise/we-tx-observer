package com.wavesenterprise.we.tx.observer.core.spring.partition

import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.we.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.transaction.Transactional

open class PartitionHandlerJpa(
    val partitionJpaRepository: TxQueuePartitionJpaRepository,
    val enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
) : PartitionHandler {

    val logger: Logger = LoggerFactory.getLogger(PartitionHandler::class.java)

    @Transactional
    override fun handleErrorWhenReading(partitionId: String) {
        partitionJpaRepository.updateErrorTxHandle(partitionId)
    }

    @Transactional
    override fun handleSuccessWhenReading(partitionId: String, txCount: Long) {
        partitionJpaRepository.updateSuccessTxHandle(partitionId)
    }

    @Transactional
    override fun pausePartitionOnTx(partitionId: String, pausedOnTxId: String) {
        enqueuedTxJpaRepository.findByIdWithLock(pausedOnTxId)
        if (partitionJpaRepository.updatePausedTxId(partitionId, pausedOnTxId) > 0) {
            logger.debug("Partition $partitionId has been paused by tx with ID = $pausedOnTxId")
        } else {
            logger.debug(
                "Partition $partitionId was not paused because tx has already became available." +
                    " TX ID = $pausedOnTxId"
            )
        }
    }

    @Transactional
    override fun handleEmptyPartition(partitionId: String) {
        logger.warn("Partition with ID = $partitionId was handled and had no NEW transactions in it")
    }

    @Transactional
    override fun resumePartitionForTx(partitionId: String, txId: String) {
        if (partitionJpaRepository.resetPausedTxId(partitionId, txId) == 0) {
            logger.warn(
                "Partition with ID = $partitionId was not reset by tx with ID = $txId" +
                    " as it is paused on other tx"
            )
        } else {
            logger.debug("Partition with ID = $partitionId has been successfully resumed by tx with ID = $txId")
        }
    }
}
