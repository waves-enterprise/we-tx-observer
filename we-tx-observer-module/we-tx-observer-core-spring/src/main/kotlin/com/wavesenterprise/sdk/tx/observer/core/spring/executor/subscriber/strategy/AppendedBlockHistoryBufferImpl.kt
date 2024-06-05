package com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber.strategy

import com.wavesenterprise.sdk.node.domain.event.BlockchainEvent

class AppendedBlockHistoryBufferImpl(
    private val maxCount: Int,
    private val maxSizeBytes: Long,
) : AppendedBlockHistoryBuffer {
    private var buffer: MutableList<BlockchainEvent.AppendedBlockHistory> = mutableListOf()
    private var bytesSize: Long = 0L

    init {
        check(maxCount >= 1) { "maxCount should be 1 or greater" }
    }

    private fun clearBuffer() {
        buffer = mutableListOf()
        bytesSize = 0L
    }

    override fun store(appendedBlockHistory: BlockchainEvent.AppendedBlockHistory): Boolean =
        if (canStore(appendedBlockHistory)) {
            buffer.add(appendedBlockHistory)
            true
        } else {
            false
        }

    private fun canStore(appendedBlockHistory: BlockchainEvent.AppendedBlockHistory): Boolean =
        buffer.size == 0 || buffer.size != maxCount && bytesSize + appendedBlockHistory.blockSize.bytesCount <= maxSizeBytes

    override fun clear(): List<BlockchainEvent.AppendedBlockHistory> =
        buffer.also {
            clearBuffer()
        }
}
