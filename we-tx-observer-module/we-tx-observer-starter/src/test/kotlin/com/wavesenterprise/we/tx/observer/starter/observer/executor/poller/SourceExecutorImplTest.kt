
package com.wavesenterprise.we.tx.observer.starter.observer.executor.poller

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.we.tx.observer.api.block.WeBlockInfo
import com.wavesenterprise.we.tx.observer.api.block.subscriber.BlockSubscriber
import com.wavesenterprise.we.tx.observer.core.spring.executor.poller.BlocksDownloadResult
import com.wavesenterprise.we.tx.observer.core.spring.executor.poller.BlocksLoader
import com.wavesenterprise.we.tx.observer.core.spring.executor.poller.SourceExecutorImpl
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.sampleCallContractTx
import com.wavesenterprise.we.tx.observer.starter.observer.util.ModelFactory.blockAtHeight
import com.wavesenterprise.we.tx.observer.starter.observer.util.ModelFactory.txList
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito

@ExtendWith(MockKExtension::class)
class SourceExecutorImplTest {

    @RelaxedMockK
    lateinit var mockSubscriber: BlockSubscriber

    @RelaxedMockK
    lateinit var blocksLoader: BlocksLoader

    @Test
    fun `empty subscribers check`() {
        val executor = SourceExecutorImpl(emptyList(), blocksLoader)
        every { blocksLoader.download(1, 2) } returns BlocksDownloadResult.NO_BLOCKS

        executor.execute(1, 2)

        verify(exactly = 0) { mockSubscriber.subscribe(any()) }
    }

    @Test
    fun `one subscriber check - zero iteration`() {
        val executor = SourceExecutorImpl(listOf(mockSubscriber), blocksLoader)
        every { blocksLoader.download(1, 2) } returns BlocksDownloadResult.NO_BLOCKS

        executor.execute(1, 2)

        verify(exactly = 0) { mockSubscriber.subscribe(any()) }
    }

    @Test
    fun `should grab blocks from current till current + window-1`() {
        val weBlockInfoCaptor = mutableListOf<WeBlockInfo>()
        val blockHeightWindow = 10L
        val currentBlockHeight = 1000L
        mockTxListForBlocksWithHeightRange(currentBlockHeight, blockHeightWindow)

        val executor = SourceExecutorImpl(
            blockSubscribers = listOf(mockSubscriber),
            blocksLoader = blocksLoader
        )
        val newCurrentHeight = executor.execute(
            blockHeightLowerBound = currentBlockHeight,
            blockHeightUpperBound = blockHeightWindow + currentBlockHeight
        )

        assertEquals(currentBlockHeight + blockHeightWindow - 1, newCurrentHeight)
        verify(exactly = 1) {
            blocksLoader.download(
                fromHeight = currentBlockHeight,
                tryToHeight = currentBlockHeight + blockHeightWindow
            )
        }

        verify(exactly = blockHeightWindow.toInt()) { mockSubscriber.subscribe(capture(weBlockInfoCaptor)) }
        (0..(blockHeightWindow - 1).toInt()).forEach { i ->
            assertEquals(weBlockInfoCaptor[i].height.value, currentBlockHeight + i)
            assertEquals(txList, weBlockInfoCaptor[i].txList.map { it.tx })
        }
    }

    @Test
    fun `should grab blocks when current node height is less than upperBound`() {
        val toBlock: Long = 3
        val fromBlock: Long = 1
        mockTxListForBlocksWithHeightRange(fromBlock, toBlock)

        val executor = SourceExecutorImpl(
            blockSubscribers = listOf(mockSubscriber),
            blocksLoader = blocksLoader
        )
        val upperBound = toBlock + 5
        val newHeight = executor.execute(fromBlock, upperBound)
        assertEquals(toBlock, newHeight)
        verify(exactly = 1) { blocksLoader.download(fromBlock, upperBound) }
    }

    @Test
    fun `should iterate over blocks in ascending order`() {
        val weBlockInfoCaptor = mutableListOf<WeBlockInfo>()
        val fromBlock = 1L
        val upperBound = 5L
        val blockHeightRange = (fromBlock..upperBound).map { height ->
            blockAtHeight(
                signature = Signature.fromByteArray("signature_$height".toByteArray()),
                height = Height(height),
                transactions = listOf(
                    sampleCallContractTx.copy(id = TxId.fromByteArray("id_$height".toByteArray()))
                )
            )
        }.toList()
        every { blocksLoader.download(any(), any()) } returns BlocksDownloadResult(blockHeightRange, false)
        val blockSubscriber: BlockSubscriber = mockk(relaxed = true)
        val executor = SourceExecutorImpl(
            blockSubscribers = listOf(blockSubscriber),
            blocksLoader = blocksLoader
        )
        val newHeight = executor.execute(fromBlock, upperBound)
        assertEquals(upperBound, newHeight)
        verify(exactly = 1) { blocksLoader.download(fromBlock, upperBound) }
        verify {
            blockSubscriber.subscribe(capture(weBlockInfoCaptor))
        }
        assertThat(weBlockInfoCaptor.size).isEqualTo(blockHeightRange.size)
        blockHeightRange.forEachIndexed { index, block ->
            val weBlockInfo = weBlockInfoCaptor[index]
            assertThat(weBlockInfo.signature).isEqualTo(block.signature)
            assertThat(weBlockInfo.height).isEqualTo(block.height)
            assertThat(weBlockInfo.txList.map { it.tx }).isEqualTo(block.transactions)
        }
    }

    @Test
    fun `one subscriber check - one iteration`() {
        mockTxListForBlocksWithHeightRange(1, 1)
        val executor = SourceExecutorImpl(
            blockSubscribers = listOf(mockSubscriber),
            blocksLoader = blocksLoader
        )
        executor.execute(1, 1)

        verify(exactly = 1) { blocksLoader.download(1, 1) }
        verify(exactly = 1) { mockSubscriber.subscribe(any()) }
    }

    @Test
    fun `one subscriber check - two iteration`() {
        mockTxListForBlocksWithHeightRange(1, 2)

        val executor = SourceExecutorImpl(listOf(mockSubscriber), blocksLoader)
        executor.execute(1, 2)

        verify(exactly = 1) { blocksLoader.download(1, 2) }
        verify(exactly = 2) { mockSubscriber.subscribe(any()) }
    }

    @Test
    fun `one subscriber and no data check`() {
        every { blocksLoader.download(any(), any()) } returns BlocksDownloadResult.NO_BLOCKS

        val executor = SourceExecutorImpl(listOf(mockSubscriber), blocksLoader)
        executor.execute(1, 2)

        verify(exactly = 1) { blocksLoader.download(1, 2) }
        verify(exactly = 0) { mockSubscriber.subscribe(any()) }
    }

    @Test
    fun `no blocks exist apart from downloaded`() {
        val executor = SourceExecutorImpl(listOf(mockSubscriber), blocksLoader)
        mockTxListForBlocksWithHeightRange(1, 1)
        val nextBlockHeight = executor.execute(1, 2)
        verify(exactly = 1) { blocksLoader.download(1, 2) }
        assertEquals(1, nextBlockHeight)
    }

    @Test
    fun `some blocks exist apart from downloaded`() {
        val executor = SourceExecutorImpl(listOf(mockSubscriber), blocksLoader)
        mockTxListForBlocksWithHeightRange(1, 1, true)
        val nextBlockHeight = executor.execute(1, 2)
        verify(exactly = 1) { blocksLoader.download(1, 2) }
        assertEquals(2, nextBlockHeight)
    }

    private fun mockTxListForBlocksWithHeightRange(fromBlock: Long, toBlock: Long, moreBlocksExists: Boolean = false) {
        val blockHeightRange =
            (fromBlock until fromBlock + toBlock).map {
                blockAtHeight(transactions = txList, height = Height(it))
            }.toList()
        every {
            blocksLoader.download(any(), any())
        } returns BlocksDownloadResult(blockHeightRange, moreBlocksExists)
    }

    private fun <T> any(type: Class<T>): T = Mockito.any<T>(type)
}
