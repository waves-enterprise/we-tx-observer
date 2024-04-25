package com.wavesenterprise.we.tx.observer.core.spring.executor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.wavesenterprise.sdk.node.client.http.tx.TxDto
import com.wavesenterprise.sdk.node.client.http.tx.TxDto.Companion.toDomain
import com.wavesenterprise.we.tx.observer.common.tx.subscriber.TxSubscriber
import com.wavesenterprise.we.tx.observer.core.spring.partition.PartitionHandler
import com.wavesenterprise.we.tx.observer.core.spring.partition.PollingTxSubscriber
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import io.micrometer.core.annotation.Timed
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

open class AppContextPollingTxSubscriber(
    val enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
    val applicationContext: ApplicationContext,
    val partitionHandler: PartitionHandler,
    val dequeSize: Int,
    val objectMapper: ObjectMapper = jacksonObjectMapper(),
) : PollingTxSubscriber {

    val logger: Logger = LoggerFactory.getLogger(AppContextPollingTxSubscriber::class.java)

    @Transactional
    @Timed(PREFIX + PARTITION_POLL_TIME)
    override fun dequeuePartitionAndSendToSubscribers(partitionId: String): Int {
        logger.debug("Polling transaction for partitionId = $partitionId")
        val actualEnqueuedTx = enqueuedTxJpaRepository.findActualEnqueuedTxForPartition(
            enqueuedTxStatus = EnqueuedTxStatus.NEW,
            partitionId = partitionId,
            pageable = PageRequest.of(0, dequeSize),
        )

        val txList = actualEnqueuedTx.content
        val availableTransactions = txList.takeWhile {
            it.available
        }
        availableTransactions.onEach { enqueuedTx ->
            val tx = objectMapper.treeToValue<TxDto>(enqueuedTx.body)
            getSubscribersFromAppContext().forEach { subscriber ->
                subscriber.subscribe(tx.toDomain())
            }
            enqueuedTx.apply { status = EnqueuedTxStatus.READ }
        }.also {
            enqueuedTxJpaRepository.saveAll(it)
        }

        txList.dropWhile { it.available }.firstOrNull()?.let {
            partitionHandler.pausePartitionOnTx(partitionId, it.id)
        }

        return availableTransactions.size.also {
            if (it > 0) {
                val lastReadTx = availableTransactions.last()
                partitionHandler.handleSuccessWhenReading(partitionId, it.toLong())
                logger.info(
                    "Polled $it transactions from persistent queue for partition with ID = $partitionId. " +
                        "Last tx position and blockHeight - " +
                        "(${lastReadTx.positionInBlock}, ${lastReadTx.blockHeight})"
                )
            }
        }
    }

    private fun getSubscribersFromAppContext(): List<TxSubscriber> =
        applicationContext.getBeansOfType(TxSubscriber::class.java).map { it.value }.toList()
}
