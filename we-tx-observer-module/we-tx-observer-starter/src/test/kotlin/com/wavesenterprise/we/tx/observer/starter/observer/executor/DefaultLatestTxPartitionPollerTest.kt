package com.wavesenterprise.we.tx.observer.starter.observer.executor

import com.wavesenterprise.we.tx.observer.api.BlockListenerException
import com.wavesenterprise.we.tx.observer.api.PartitionHandlingException
import com.wavesenterprise.we.tx.observer.core.spring.partition.DefaultLatestTxPartitionPoller
import com.wavesenterprise.we.tx.observer.core.spring.partition.PartitionHandler
import com.wavesenterprise.we.tx.observer.core.spring.partition.PollingTxSubscriber
import com.wavesenterprise.we.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.lang.IllegalArgumentException

@ExtendWith(MockKExtension::class)
internal class DefaultLatestTxPartitionPollerTest {

    @RelaxedMockK
    lateinit var txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository

    @RelaxedMockK
    lateinit var partitionHandler: PartitionHandler

    @MockK
    lateinit var pollingTxSubscriber: PollingTxSubscriber

    @InjectMockKs
    lateinit var defaultLatestTxPartitionPoller: DefaultLatestTxPartitionPoller

    @Test
    fun `should get latest actual partition and handle it with success`() {
        val partitionId = "partId"
        every { pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(any()) } returns 1
        every { txQueuePartitionJpaRepository.findAndLockLatestActualPartition() } returns partitionId

        defaultLatestTxPartitionPoller.pollLatestActualPartition()

        verifyOrder {
            txQueuePartitionJpaRepository.findAndLockLatestActualPartition()
            pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(partitionId)
        }
        confirmVerified(txQueuePartitionJpaRepository, partitionHandler)
    }

    @Test
    fun `should do nothing when having no actual partitions`() {
        every { txQueuePartitionJpaRepository.findAndLockLatestActualPartition() } returns null

        defaultLatestTxPartitionPoller.pollLatestActualPartition()

        verify(exactly = 1) {
            txQueuePartitionJpaRepository.findAndLockLatestActualPartition()
        }
        confirmVerified(txQueuePartitionJpaRepository, pollingTxSubscriber, partitionHandler)
    }

    @ParameterizedTest
    @MethodSource("exceptions")
    fun `should wrap with PartitionHandlingException for partitions with Exceptions`(
        exception: Exception
    ) {
        val partitionId = "partId"
        every { txQueuePartitionJpaRepository.findAndLockLatestActualPartition() } returns partitionId
        every {
            pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(partitionId)
        } throws exception

        val partitionHandlingException =
            assertThrows<PartitionHandlingException> { defaultLatestTxPartitionPoller.pollLatestActualPartition() }

        partitionHandlingException.also {
            assertEquals(partitionId, it.partitionId)
            assertEquals(exception, it.cause)
        }
        verifyOrder {
            txQueuePartitionJpaRepository.findAndLockLatestActualPartition()
            pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(partitionId)
        }
        confirmVerified(txQueuePartitionJpaRepository, pollingTxSubscriber, partitionHandler)
    }

    @Test
    fun `should handle empty partition`() {
        val partitionId = "partId"
        every { txQueuePartitionJpaRepository.findAndLockLatestActualPartition() } returns partitionId
        every {
            pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(partitionId)
        } returns 0

        defaultLatestTxPartitionPoller.pollLatestActualPartition()

        verifyOrder {
            txQueuePartitionJpaRepository.findAndLockLatestActualPartition()
            pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(partitionId)
            partitionHandler.handleEmptyPartition(partitionId)
        }
        confirmVerified(txQueuePartitionJpaRepository, pollingTxSubscriber, partitionHandler)
    }

    companion object {

        @JvmStatic
        fun exceptions() = setOf(
            BlockListenerException("bla bla", Exception()),
            IllegalArgumentException()
        ).map { Arguments.of(it) }.stream()
    }
}
