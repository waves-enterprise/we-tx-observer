package com.wavesenterprise.we.tx.observer.core.spring.partition

import com.wavesenterprise.we.tx.observer.api.BlockListenerException
import com.wavesenterprise.we.tx.observer.api.PartitionHandlingException
import com.wavesenterprise.we.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

open class DefaultLatestTxPartitionPoller(
    val txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository,
    val pollingTxSubscriber: PollingTxSubscriber,
    val partitionHandler: PartitionHandler,
) : LatestTxPartitionPoller {

    private val logger: Logger = LoggerFactory.getLogger(DefaultLatestTxPartitionPoller::class.java)

    @Transactional
    override fun pollLatestActualPartition(): String? =
        txQueuePartitionJpaRepository.findAndLockLatestActualPartition()?.also {
            logger.debug("Started polling partition with ID = $it")
            tryToDequeueTxForPartition(it)
            logger.debug("Finished polling partition with ID = $it")
        }.also {
            it ?: logger.debug("No actual partitions found to be polled")
        }

    private fun tryToDequeueTxForPartition(partitionId: String) {
        try {
            val newTxCountForThisPartition = pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(partitionId)
            if (newTxCountForThisPartition == 0) {
                partitionHandler.handleEmptyPartition(partitionId)
            }
        } catch (blockListenerException: BlockListenerException) {
            logger.error("Error in one of the tx handlers for partition with id = $partitionId")
            throw PartitionHandlingException(partitionId, blockListenerException)
        } catch (e: Exception) {
            logger.error("Error while handling partition with id = $partitionId", e)
            throw PartitionHandlingException(partitionId, e)
        }
    }
}
