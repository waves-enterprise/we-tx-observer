package com.wavesenterprise.sdk.tx.observer.starter.observer.executor.poller

import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.poller.BlocksLoaderImpl
import com.wavesenterprise.sdk.tx.observer.starter.observer.util.ModelFactory.blockAtHeight
import com.wavesenterprise.sdk.tx.observer.starter.observer.util.ModelFactory.blockHeaders
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class BlocksLoaderTest {

    @Mock
    lateinit var blocksService: BlocksService

    @Test
    fun `download when max window size more than blocks`() {
        val from = 1L
        val to = 10L
        val windowsSize = 1000000L
        mockBlockHeaders(from, to, 100L)
        mockBlocks(from, to)

        val result = BlocksLoaderImpl(blocksService, windowsSize).download(from, to)
        assertEquals(10, result.blocks.size)
        assertFalse(result.moreBlocksExist)
    }

    @Test
    fun `download when max window size less than blocks sum`() {
        val from = 1L
        val to = 10L
        val windowsSize = 550L
        mockBlockHeaders(from, to, 100L)
        mockBlocks(from, 5)

        val result = BlocksLoaderImpl(blocksService, windowsSize).download(from, to)
        assertEquals(5, result.blocks.size)
        assertTrue(result.moreBlocksExist)
    }

    @Test
    fun `download when first block size more than window`() {
        val from = 1L
        val to = 10L
        val windowsSize = 100L
        mockBlockHeaders(from, to, 200L)
        mockBlocks(from, 1)

        val result = BlocksLoaderImpl(blocksService, windowsSize).download(from, to)
        assertEquals(1, result.blocks.size)
        assertTrue(result.moreBlocksExist)
    }

    @Test
    fun `download when second block doesn't fit in window`() {
        val from = 1L
        val to = 2L
        val windowSize = 300L

        mockBlockHeaders(from, to, 160L)
        mockBlocks(from, 1)

        val result = BlocksLoaderImpl(blocksService, windowSize).download(from, to)
        assertEquals(1, result.blocks.size)
        assertTrue(result.moreBlocksExist)
    }

    private fun mockBlockHeaders(from: Long, to: Long, blockSize: Long) {
        `when`(blocksService.blockHeadersSequence(from, to))
            .thenReturn(
                (from until to + 1).map {
                    blockHeaders(
                        blockSize = blockSize,
                        height = Height.fromLong(it)
                    )
                }
            )
    }

    private fun mockBlocks(from: Long, to: Long) {
        `when`(blocksService.blockSequence(from, to))
            .thenReturn(
                (from until to + 1).map {
                    blockAtHeight(
                        height = Height.fromLong(it)
                    )
                }
            )
    }
}
