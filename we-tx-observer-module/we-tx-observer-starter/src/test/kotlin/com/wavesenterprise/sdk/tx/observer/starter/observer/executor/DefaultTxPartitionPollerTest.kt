package com.wavesenterprise.sdk.tx.observer.starter.observer.executor

import com.wavesenterprise.sdk.tx.observer.api.BlockListenerException
import com.wavesenterprise.sdk.tx.observer.api.PartitionHandlingException
import com.wavesenterprise.sdk.tx.observer.core.spring.partition.DefaultTxPartitionPoller
import com.wavesenterprise.sdk.tx.observer.core.spring.partition.PartitionHandler
import com.wavesenterprise.sdk.tx.observer.core.spring.partition.PollingTxSubscriber
import com.wavesenterprise.sdk.tx.observer.core.spring.properties.PartitionPollerConfig
import com.wavesenterprise.sdk.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.sdk.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.sdk.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.lang.IllegalArgumentException

@ExtendWith(MockKExtension::class)
internal class DefaultTxPartitionPollerTest {

    @RelaxedMockK
    lateinit var txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository

    @RelaxedMockK
    lateinit var partitionHandler: PartitionHandler

    @MockK
    lateinit var pollingTxSubscriber: PollingTxSubscriber

    @MockK
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    @MockK
    lateinit var partitionPollerProperties: PartitionPollerConfig

    @InjectMockKs
    lateinit var defaultTxPartitionPoller: DefaultTxPartitionPoller

    private val accelerateAtQueueSize = 200L

    @BeforeEach
    fun init() {
        every { enqueuedTxJpaRepository.countByStatus(EnqueuedTxStatus.NEW) } returns accelerateAtQueueSize - 1
        every { partitionPollerProperties.accelerateAtQueueSize } returns accelerateAtQueueSize
    }

    @Test
    fun `should get latest actual partition and handle it with success`() {
        val partitionId = "partId"
        every { partitionPollerProperties.accelerateAtQueueSize } returns accelerateAtQueueSize
        every { pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(any()) } returns 1
        every { txQueuePartitionJpaRepository.findAndLockLatestPartition() } returns partitionId

        defaultTxPartitionPoller.pollPartition()

        verifyOrder {
            txQueuePartitionJpaRepository.findAndLockLatestPartition()
            pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(partitionId)
        }
        confirmVerified(txQueuePartitionJpaRepository, partitionHandler)
    }

    @Test
    fun `should get random actual partition and handle it with success`() {
        val partitionId = "partId"
        every { enqueuedTxJpaRepository.countByStatus(EnqueuedTxStatus.NEW) } returns accelerateAtQueueSize + 1
        every { pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(any()) } returns 1
        every { txQueuePartitionJpaRepository.findAndLockRandomPartition() } returns partitionId

        defaultTxPartitionPoller.pollPartition()

        verifyOrder {
            txQueuePartitionJpaRepository.findAndLockRandomPartition()
            pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(partitionId)
        }
        confirmVerified(txQueuePartitionJpaRepository, partitionHandler)
    }

    @Test
    fun `should do nothing when having no actual partitions`() {
        every { txQueuePartitionJpaRepository.findAndLockLatestPartition() } returns null

        defaultTxPartitionPoller.pollPartition()

        verify(exactly = 1) {
            txQueuePartitionJpaRepository.findAndLockLatestPartition()
        }
        confirmVerified(txQueuePartitionJpaRepository, pollingTxSubscriber, partitionHandler)
    }

    @ParameterizedTest
    @MethodSource("exceptions")
    fun `should wrap with PartitionHandlingException for partitions with Exceptions`(
        exception: Exception
    ) {
        val partitionId = "partId"
        every { txQueuePartitionJpaRepository.findAndLockLatestPartition() } returns partitionId
        every {
            pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(partitionId)
        } throws exception

        val partitionHandlingException =
            assertThrows<PartitionHandlingException> { defaultTxPartitionPoller.pollPartition() }

        partitionHandlingException.also {
            assertEquals(partitionId, it.partitionId)
            assertEquals(exception, it.cause)
        }
        verifyOrder {
            txQueuePartitionJpaRepository.findAndLockLatestPartition()
            pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(partitionId)
        }
        confirmVerified(txQueuePartitionJpaRepository, pollingTxSubscriber, partitionHandler)
    }

    @Test
    fun `should handle empty partition`() {
        val partitionId = "partId"
        every { txQueuePartitionJpaRepository.findAndLockLatestPartition() } returns partitionId
        every {
            pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(partitionId)
        } returns 0

        defaultTxPartitionPoller.pollPartition()

        verifyOrder {
            txQueuePartitionJpaRepository.findAndLockLatestPartition()
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
