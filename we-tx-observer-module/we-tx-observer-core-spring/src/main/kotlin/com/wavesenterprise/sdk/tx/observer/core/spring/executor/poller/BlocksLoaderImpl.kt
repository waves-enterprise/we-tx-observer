package com.wavesenterprise.sdk.tx.observer.core.spring.executor.poller

import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.sdk.node.domain.blocks.BlockHeaders
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.poller.BlocksDownloadResult.Companion.NO_BLOCKS
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.poller.SourceExecutorImpl.Companion.BLOCKS_EXECUTOR_LOGGER_NAME
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class BlocksLoaderImpl(
    private val blocksService: BlocksService,
    private val downloadWindowSize: Long,
) : BlocksLoader {

    private val logger: Logger = LoggerFactory.getLogger(BLOCKS_EXECUTOR_LOGGER_NAME)

    override fun download(fromHeight: Long, tryToHeight: Long): BlocksDownloadResult {
        logger.debug("Start download blocks process. FromHeight: $fromHeight, TryToHeight: $tryToHeight")
        val blockHeaders = blocksService.blockHeadersSequence(fromHeight, tryToHeight)
        if (blockHeaders.isEmpty()) return NO_BLOCKS
        val iterator = blockHeaders.iterator()
        var lastBlock: BlockHeaders = iterator.next()
        var windowSize = lastBlock.blockSize
        var hasMoreBlocks = false
        if (windowSize > downloadWindowSize) {
            logger.warn("Block ${lastBlock.height} has size ${lastBlock.blockSize}, it`s more than window")
        }
        while (windowSize < downloadWindowSize && iterator.hasNext()) {
            val buf = iterator.next()
            windowSize += lastBlock.blockSize
            if (windowSize < downloadWindowSize) {
                lastBlock = buf
            } else {
                hasMoreBlocks = true
            }
        }
        logger.debug("Try to download blocks. FromHeight: $fromHeight, toHeight: ${lastBlock.height}")
        return BlocksDownloadResult(
            blocksService.blockSequence(fromHeight, lastBlock.height.value).also {
                logger.debug("Blocks from height $fromHeight to ${lastBlock.height} downloaded")
            },
            hasMoreBlocks || iterator.hasNext(),
        )
    }
}
