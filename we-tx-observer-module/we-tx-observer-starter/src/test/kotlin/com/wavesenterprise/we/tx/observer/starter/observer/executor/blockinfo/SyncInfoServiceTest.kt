package com.wavesenterprise.we.tx.observer.starter.observer.executor.blockinfo

import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.BlockHistoryService
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfo
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfoServiceImpl
import com.wavesenterprise.we.tx.observer.core.spring.metrics.MetricsContainer
import com.wavesenterprise.we.tx.observer.domain.BlockHeightInfo
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.we.tx.observer.jpa.repository.BlockHeightJpaRepository
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.we.tx.observer.starter.observer.util.ModelFactory.blockAtHeight
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@ExtendWith(MockKExtension::class)
class SyncInfoServiceTest {
    @MockK
    lateinit var blocksService: BlocksService

    @MockK
    lateinit var blockHeightJpaRepository: BlockHeightJpaRepository

    @MockK
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    @RelaxedMockK
    lateinit var blockHistoryService: BlockHistoryService

    @RelaxedMockK
    lateinit var nodeHeightMetric: MetricsContainer

    @RelaxedMockK
    lateinit var observerHeightMetric: MetricsContainer

    @ParameterizedTest
    @ValueSource(longs = [10000L, 10001L])
    fun `should pause sync when NEW transactions count greater than or equal to pauseSyncAtQueueSize`(
        newEnqueuedTxCount: Long,
    ) {
        val observerHeight = 100L
        val nodeHeight = 1000L
        val prevBlockSignature = Signature.fromByteArray("VJFGHc".toByteArray())
        every { blocksService.blockHeight() } returns Height(nodeHeight)
        every { enqueuedTxJpaRepository.countByStatus(EnqueuedTxStatus.NEW) } returns newEnqueuedTxCount
        every { blockHeightJpaRepository.findAll() } returns listOf(
            BlockHeightInfo(
                currentHeight = observerHeight,
                prevBlockSignature = prevBlockSignature.asBase58String()
            )
        )

        val syncInfo = syncInfo()

        assertThat(syncInfo.nodeHeight.value).isEqualTo(nodeHeight)
        assertThat(syncInfo.observerHeight.value).isEqualTo(observerHeight)
        assertThat(syncInfo.prevBlockSignature).isEqualTo(prevBlockSignature)
        verify(exactly = 0) { blockHeightJpaRepository.save(any()) }
    }

    @Test
    fun `should start with activation height if enabled and observer height = activation height`() {
        val activationHeight = 20L
        val signature = Signature.fromByteArray("VJFGHc".toByteArray())
        every { blocksService.blockHeight() } returns Height(100L)
        every { enqueuedTxJpaRepository.countByStatus(EnqueuedTxStatus.NEW) } returns 100L
        every { blockHeightJpaRepository.save(any()) } answers { firstArg() }
        every { blockHeightJpaRepository.findAll() } returns emptyList()
        every { blocksService.blockAtHeight(activationHeight - 1) } returns blockAtHeight(
            signature = signature
        )

        val syncInfo = syncInfo(activationHeight = activationHeight)

        assertThat(syncInfo.observerHeight.value).isEqualTo(activationHeight)
        assertThat(syncInfo.prevBlockSignature).isEqualTo(signature)
    }

    @Test
    fun `should reset height to stable node height if enabled and node height is less than observer height`() {
        val signature = Signature.fromByteArray("JiLqI92".toByteArray())
        val nodeHeight = 57L
        every { blocksService.blockHeight() } returns Height(nodeHeight)
        every { enqueuedTxJpaRepository.countByStatus(EnqueuedTxStatus.NEW) } returns 100L
        every { blockHeightJpaRepository.save(any()) } answers { firstArg() }
        every { blockHeightJpaRepository.findAll() } returns listOf(
            BlockHeightInfo(
                currentHeight = nodeHeight + 1,
                prevBlockSignature = "yDgpD7gu"
            )
        )
        every { blocksService.blockAtHeight(nodeHeight - 1) } returns blockAtHeight(
            signature = signature
        )

        val syncInfo = syncInfo(autoResetHeight = true)

        assertThat(syncInfo.observerHeight.value).isEqualTo(nodeHeight)
        assertThat(syncInfo.prevBlockSignature).isEqualTo(signature)
    }

    @Test
    fun `should use observer height if no need to reset or sync`() {
        val observerHeight = 130L
        val nodeHeight = 180L
        val signature = Signature.fromByteArray("hiD50hVO".toByteArray())
        every { blocksService.blockHeight() } returns Height(nodeHeight)
        every { enqueuedTxJpaRepository.countByStatus(EnqueuedTxStatus.NEW) } returns 100L
        every { blockHeightJpaRepository.save(any()) } answers { firstArg() }
        every { blockHeightJpaRepository.findAll() } returns listOf(
            BlockHeightInfo(
                currentHeight = observerHeight,
                prevBlockSignature = signature.asBase58String()
            )
        )
        every { blocksService.blockAtHeight(nodeHeight) } returns blockAtHeight()

        val syncInfo = syncInfo()

        assertThat(syncInfo.observerHeight.value).isEqualTo(observerHeight)
        assertThat(syncInfo.prevBlockSignature).isEqualTo(signature)
    }

    private fun syncInfo(
        activationHeight: Long = 1L,
        syncHistory: Boolean = true,
        pauseSyncAtQueueSize: Long = 10000L,
        autoResetHeight: Boolean = false,
        forkNotResolvedHeightDrop: Long = 10,
    ): SyncInfo {
        return syncInfoService(
            activationHeight,
            syncHistory,
            pauseSyncAtQueueSize,
            autoResetHeight,
            forkNotResolvedHeightDrop,
        ).syncInfo()
    }

    private fun syncInfoService(
        activationHeight: Long,
        syncHistory: Boolean,
        pauseSyncAtQueueSize: Long,
        autoResetHeight: Boolean,
        forkNotResolvedHeightDrop: Long,
    ): SyncInfoService = SyncInfoServiceImpl(
        blockHeightJpaRepository = blockHeightJpaRepository,
        enqueuedTxJpaRepository = enqueuedTxJpaRepository,
        blockHistoryService = blockHistoryService,
        blocksService = blocksService,
        syncHistory = SyncInfoServiceImpl.SyncHistoryProperties(
            enabled = syncHistory,
            fromHeight = activationHeight,
            pauseSyncAtQueueSize = pauseSyncAtQueueSize,
        ),
        autoResetHeight = autoResetHeight,
        forkNotResolvedHeightDrop = forkNotResolvedHeightDrop,
        nodeHeightMetric = nodeHeightMetric,
        observerHeightMetric = observerHeightMetric
    )
}
