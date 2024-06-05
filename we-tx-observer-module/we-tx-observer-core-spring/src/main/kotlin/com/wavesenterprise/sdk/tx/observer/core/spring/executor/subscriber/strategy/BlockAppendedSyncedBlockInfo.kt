package com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber.strategy

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.Timestamp
import com.wavesenterprise.sdk.node.domain.event.BlockchainEvent
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo.SyncedBlockInfo

class BlockAppendedSyncedBlockInfo(
    private val blockAppended: BlockchainEvent.BlockAppended,
) : SyncedBlockInfo {
    override val signature: Signature
        get() = blockAppended.signature
    override val height: Height
        get() = blockAppended.height
    override val timestamp: Timestamp
        get() = blockAppended.timestamp
}

fun BlockchainEvent.BlockAppended.toSyncedBlockInfo(): SyncedBlockInfo =
    BlockAppendedSyncedBlockInfo(this)
