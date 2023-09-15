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
import com.wavesenterprise.we.tx.observer.jpa.repository.BlockHeightJpaRepository
import com.wavesenterprise.we.tx.observer.starter.observer.util.ModelFactory.blockAtHeight
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SyncInfoServiceTest {
    @MockK
    lateinit var blocksService: BlocksService

    @MockK
    lateinit var blockHeightJpaRepository: BlockHeightJpaRepository

    @RelaxedMockK
    lateinit var blockHistoryService: BlockHistoryService

    @RelaxedMockK
    lateinit var nodeHeightMetric: MetricsContainer

    @RelaxedMockK
    lateinit var observerHeightMetric: MetricsContainer

    @Test
    fun `should start with activation height if enabled and observer height = activation height`() {
        val activationHeight = 20L
        val signature = Signature.fromByteArray("VJFGHc".toByteArray())
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
        every { blockHeightJpaRepository.findAll() } returns listOf(
            BlockHeightInfo(
                currentHeight = nodeHeight + 1,
                prevBlockSignature = "yDgpD7gu"
            )
        )
        every { blocksService.blockAtHeight(nodeHeight - 1) } returns blockAtHeight(
            signature = signature
        )

        val syncInfo = syncInfo(nodeHeight = nodeHeight, autoResetHeight = true)

        assertThat(syncInfo.observerHeight.value).isEqualTo(nodeHeight)
        assertThat(syncInfo.prevBlockSignature).isEqualTo(signature)
    }

    @Test
    fun `should use observer height if no need to reset or sync`() {
        val observerHeight = 130L
        val signature = Signature.fromByteArray("hiD50hVO".toByteArray())
        val nodeHeight = 180L
        every { blockHeightJpaRepository.findAll() } returns listOf(
            BlockHeightInfo(
                currentHeight = observerHeight,
                prevBlockSignature = signature.asBase58String()
            )
        )
        every { blocksService.blockAtHeight(nodeHeight) } returns blockAtHeight()

        val syncInfo = syncInfo(nodeHeight = nodeHeight)

        assertThat(syncInfo.observerHeight.value).isEqualTo(observerHeight)
        assertThat(syncInfo.prevBlockSignature).isEqualTo(signature)
    }

    private fun syncInfo(
        nodeHeight: Long = 100L,
        activationHeight: Long = 1L,
        syncHistory: Boolean = true,
        autoResetHeight: Boolean = false,
        forkNotResolvedHeightDrop: Long = 10,
    ): SyncInfo {
        every { blocksService.blockHeight() } returns Height(nodeHeight)
        every { blockHeightJpaRepository.save(any()) } answers {
            arg(0)
        }
        return syncInfoService(
            activationHeight,
            syncHistory,
            autoResetHeight,
            forkNotResolvedHeightDrop,
        ).syncInfo()
    }

    private fun syncInfoService(
        activationHeight: Long,
        syncHistory: Boolean,
        autoResetHeight: Boolean,
        forkNotResolvedHeightDrop: Long,
    ): SyncInfoService = SyncInfoServiceImpl(
        blockHeightJpaRepository = blockHeightJpaRepository,
        blockHistoryService = blockHistoryService,
        blocksService = blocksService,
        syncHistory = SyncInfoServiceImpl.SyncHistoryProperties(
            enabled = syncHistory,
            fromHeight = activationHeight
        ),
        autoResetHeight = autoResetHeight,
        forkNotResolvedHeightDrop = forkNotResolvedHeightDrop,
        nodeHeightMetric = nodeHeightMetric,
        observerHeightMetric = observerHeightMetric
    )
}
