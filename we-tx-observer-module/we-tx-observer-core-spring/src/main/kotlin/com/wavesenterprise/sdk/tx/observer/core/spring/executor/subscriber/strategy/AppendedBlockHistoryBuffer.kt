package com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber.strategy

import com.wavesenterprise.sdk.node.domain.event.BlockchainEvent

interface AppendedBlockHistoryBuffer {
    fun store(appendedBlockHistory: BlockchainEvent.AppendedBlockHistory): Boolean
    fun clear(): List<BlockchainEvent.AppendedBlockHistory>
}
