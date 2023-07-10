package com.wavesenterprise.we.tx.observer.core.spring.executor.poller

import com.wavesenterprise.we.tx.observer.api.block.subscriber.BlockSubscriber
import com.wavesenterprise.we.tx.observer.core.spring.component.HttpApiWeBlockInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SourceExecutorImpl(
    val blockSubscribers: List<BlockSubscriber>,
    val blocksLoader: BlocksLoader,
) : SourceExecutor {

    private val logger: Logger = LoggerFactory.getLogger(BLOCKS_EXECUTOR_LOGGER_NAME)

    override fun execute(blockHeightLowerBound: Long, blockHeightUpperBound: Long): Long {
        val (blocksAtHeightRange, moreBlocksExists) = blocksLoader.download(
            fromHeight = blockHeightLowerBound,
            tryToHeight = blockHeightUpperBound
        )
        val nextBlockHeight = when {
            blocksAtHeightRange.isEmpty() -> blockHeightLowerBound
            moreBlocksExists -> blocksAtHeightRange.last().height.value + 1
            else -> blocksAtHeightRange.last().height.value
        }
        if (blockSubscribers.isEmpty()) {
            logger.warn("Empty subscriber list in SourceExecutor! Skipping...")
            return nextBlockHeight
        }
        for (blocksAtHeightDto in blocksAtHeightRange) {
            logger.debug(
                "current block height = ${blocksAtHeightDto.height}; " +
                    "tx count in block = ${blocksAtHeightDto.transactionCount} "
            )

            blockSubscribers.forEach {
                it.subscribe(HttpApiWeBlockInfo(blocksAtHeightDto))
            }
        }
        return nextBlockHeight
    }

    companion object {
        const val BLOCKS_EXECUTOR_LOGGER_NAME = "BlockExecutorLogger"
    }
}
