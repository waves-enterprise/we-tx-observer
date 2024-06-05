package com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber.strategy

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.Timestamp
import com.wavesenterprise.sdk.node.domain.event.BlockchainEvent
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo.SyncedBlockInfo

class AppendedBlockHistorySyncedBlockInfo(
    private val appendedBlockHistory: BlockchainEvent.AppendedBlockHistory,
) : SyncedBlockInfo {
    override val signature: Signature
        get() = appendedBlockHistory.signature
    override val height: Height
        get() = appendedBlockHistory.height
    override val timestamp: Timestamp
        get() = appendedBlockHistory.timestamp
}

fun BlockchainEvent.AppendedBlockHistory.toSyncedBlockInfo(): SyncedBlockInfo =
    AppendedBlockHistorySyncedBlockInfo(this)
