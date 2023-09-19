package com.wavesenterprise.we.tx.observer.starter.observer.executor.poller

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.we.tx.observer.core.spring.executor.poller.ScheduledBlockInfoSynchronizer
import com.wavesenterprise.we.tx.observer.core.spring.executor.poller.ScheduledBlockInfoSynchronizer.Companion.OFFSET
import com.wavesenterprise.we.tx.observer.core.spring.executor.poller.SourceExecutor
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfo
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.we.tx.observer.starter.observer.util.TxExecutorStub
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.lang.Long.min

@ExtendWith(MockKExtension::class)
internal class ScheduledWeBlockInfoSynchronizerTest {

    @MockK
    lateinit var sourceExecutor: SourceExecutor

    @MockK
    lateinit var syncInfoService: SyncInfoService

    private val blockHeightWindow = 10L

    private lateinit var scheduledBlockInfoSynchronizer: ScheduledBlockInfoSynchronizer

    @BeforeEach
    fun setUp() {
        scheduledBlockInfoSynchronizer = ScheduledBlockInfoSynchronizer(
            sourceExecutor = sourceExecutor,
            syncInfoService = syncInfoService,
            liquidBlockPollingDelay = 0,
            blockHeightWindow = blockHeightWindow,
            txExecutor = TxExecutorStub
        )
    }

    @Test
    fun `should sync from observer height to node height using window`() {
        val observerHeight = 3L
        val nodeHeight = 50L
        val syncInfo: SyncInfo = mockk {
            every { this@mockk.observerHeight } returns Height(observerHeight)
            every { this@mockk.nodeHeight } returns Height(nodeHeight)
        }
        every { syncInfoService.syncInfo() } returns syncInfo
        every { syncInfoService.syncedTo(any(), any()) } returns Unit
        every { sourceExecutor.execute(any(), any()) } answers { arg(1) as Long + 1 }

        scheduledBlockInfoSynchronizer.syncNodeBlockInfo()

        verifyOrder {
            syncInfoService.syncInfo()
            sectionsOfHeight(observerHeight, nodeHeight).forEach { section ->
                sourceExecutor.execute(section.first, section.second)
                syncInfoService.syncedTo(section.second + 1)
            }
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
        every { sourceExecutor.execute(any(), any()) } answers { arg(1) as Long }

        scheduledBlockInfoSynchronizer.syncNodeBlockInfo()

        verifyOrder {
            syncInfoService.syncInfo()
            sourceExecutor.execute(stableNodeHeight, nodeHeight + 1)
            syncInfoService.syncedTo(nodeHeight + 1)
        }
    }

    private fun sectionsOfHeight(
        observerHeight: Long,
        nodeHeight: Long
    ): Sequence<Pair<Long, Long>> =
        generateSequence(observerHeight) { it + blockHeightWindow + 1 }
            .map { first -> first to first + blockHeightWindow }
            .takeWhile { section ->
                section.first < nodeHeight
            }
            .map { section ->
                section.first to min(section.second, nodeHeight + OFFSET)
            }
}
