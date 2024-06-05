package com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber.strategy

import com.wavesenterprise.sdk.node.domain.event.BlockchainEvent
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber.strategy.Action.HandleBlocks
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber.toWeBlockInfo

fun AppendedBlockHistoryBuffer.clearAndBuildHandleBlocks(): HandleBlocks =
    clear().toHandleBlocks()

fun List<BlockchainEvent.AppendedBlockHistory>.toHandleBlocks(): HandleBlocks =
    HandleBlocks(
        weBlockInfos = map { it.toWeBlockInfo() },
        syncedBlockInfos = map { it.toSyncedBlockInfo() }
    )
