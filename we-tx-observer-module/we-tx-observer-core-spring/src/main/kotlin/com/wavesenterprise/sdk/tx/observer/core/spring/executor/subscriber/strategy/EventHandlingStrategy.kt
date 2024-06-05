package com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber.strategy

import com.wavesenterprise.sdk.node.domain.event.BlockchainEvent
import com.wavesenterprise.sdk.tx.observer.api.block.WeBlockInfo
import com.wavesenterprise.sdk.tx.observer.api.block.WeRollbackInfo
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo.SyncedBlockInfo

interface EventHandlingStrategy {
    fun actionsOn(event: BlockchainEvent): List<Action>
}

sealed class Action {
    class HandleBlocks(
        val weBlockInfos: List<WeBlockInfo>,
        val syncedBlockInfos: List<SyncedBlockInfo> = emptyList(),
    ) : Action() {
        constructor(
            weBlockInfo: WeBlockInfo,
            syncedBlockInfo: SyncedBlockInfo? = null,
        ) : this(
            listOf(weBlockInfo),
            syncedBlockInfo?.let(::listOf) ?: emptyList()
        )
        constructor(
            weBlockInfos: Sequence<WeBlockInfo>,
            syncedBlockInfos: Sequence<SyncedBlockInfo> = emptySequence(),
        ) : this(
            weBlockInfos.toList(),
            syncedBlockInfos.toList()
        )
    }
    class HandleRollback(val weRollbackInfo: WeRollbackInfo) : Action()
}
