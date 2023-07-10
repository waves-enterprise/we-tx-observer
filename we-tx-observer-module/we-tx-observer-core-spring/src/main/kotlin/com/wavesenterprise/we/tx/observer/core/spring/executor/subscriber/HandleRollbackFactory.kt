package com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber

import com.wavesenterprise.sdk.node.domain.event.BlockchainEvent
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.strategy.Action.HandleRollback

interface HandleRollbackFactory {
    fun create(event: BlockchainEvent.RollbackCompleted): HandleRollback
}
