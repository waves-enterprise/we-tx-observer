package com.wavesenterprise.we.tx.observer.starter.observer.executor.poller

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.we.tx.observer.core.spring.executor.poller.ScheduledBlockInfoSynchronizer
import com.wavesenterprise.we.tx.observer.core.spring.executor.poller.SourceExecutor
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfo
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.we.tx.observer.starter.observer.util.TxExecutorStub
import com.wavesenterprise.we.tx.observer.starter.properties.TxObserverProperties
import io.mockk.called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class ScheduledWeBlockInfoSynchronizerTest {

    @MockK
    lateinit var sourceExecutor: SourceExecutor

    @MockK
    lateinit var syncInfoService: SyncInfoService

    @MockK
    lateinit var txObserverProperties: TxObserverProperties

    @MockK
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    private val pauseSyncAtQueueSize = 100L
    private val blockHeightWindow = 10L

    private lateinit var scheduledBlockInfoSynchronizer: ScheduledBlockInfoSynchronizer

    @BeforeEach
    fun setUp() {
        every { txObserverProperties.pauseSyncAtQueueSize } returns pauseSyncAtQueueSize
        every { txObserverProperties.blockHeightWindow } returns blockHeightWindow
        every { txObserverProperties.liquidBlockPollingDelay } returns 0
        scheduledBlockInfoSynchronizer = ScheduledBlockInfoSynchronizer(
            sourceExecutor = sourceExecutor,
            syncInfoService = syncInfoService,
            enqueuedTxJpaRepository = enqueuedTxJpaRepository,
            txObserverConfig = txObserverProperties,
            txExecutor = TxExecutorStub
        )
    }

    @Test
    fun `should pause sync when queue size too big`() {
        every { enqueuedTxJpaRepository.countByStatus(EnqueuedTxStatus.NEW) } returns pauseSyncAtQueueSize

        scheduledBlockInfoSynchronizer.syncNodeBlockInfo()

        verify { syncInfoService wasNot called }
    }

    @Test
    fun `should sync from observer height to height plus window `() {
        val observerHeight = 3L
        val nodeHeight = 50L
        val syncInfo: SyncInfo = mockk {
            every { this@mockk.observerHeight } returns Height(observerHeight)
            every { this@mockk.nodeHeight } returns Height(nodeHeight)
        }
        every { syncInfoService.syncInfo() } returns syncInfo
        every { syncInfoService.syncedTo(any(), any()) } returns Unit
        every { enqueuedTxJpaRepository.countByStatus(any()) } returns pauseSyncAtQueueSize - 1
        every { sourceExecutor.execute(any(), any()) } answers { arg(1) as Long + 1 }

        scheduledBlockInfoSynchronizer.syncNodeBlockInfo()

        verifyOrder {
            syncInfoService.syncInfo()
            sourceExecutor.execute(observerHeight, observerHeight + blockHeightWindow)
            syncInfoService.syncedTo(observerHeight + blockHeightWindow + 1)
        }
    }

    @Test
    fun `should sync from stable node height when observer height equals node height`() {
        val observerHeight = 15L
        val nodeHeight = observerHeight
        val stableNodeHeight = nodeHeight - 1
        val syncInfo: SyncInfo = mockk {
            every { this@mockk.observerHeight } returns Height(observerHeight)
            every { this@mockk.nodeHeight } returns Height(nodeHeight)
        }
        every { syncInfoService.syncInfo() } returns syncInfo
        every { syncInfoService.syncedTo(any(), any()) } returns Unit
        every { enqueuedTxJpaRepository.countByStatus(any()) } returns pauseSyncAtQueueSize - 1
        every { sourceExecutor.execute(any(), any()) } answers { arg(1) as Long }

        scheduledBlockInfoSynchronizer.syncNodeBlockInfo()

        verifyOrder {
            syncInfoService.syncInfo()
            sourceExecutor.execute(stableNodeHeight, nodeHeight + 1)
            syncInfoService.syncedTo(nodeHeight + 1)
        }
    }
}
