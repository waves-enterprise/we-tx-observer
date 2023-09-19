package com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.strategy

import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.HandleRollbackFactory

class BlockAppendedEventHandlingStrategyFactory(
    private val handleRollbackFactory: HandleRollbackFactory,
    private val appendedBlockHistoryBuffer: AppendedBlockHistoryBuffer,
) : EventHandlingStrategyFactory {
    override fun create(height: Long): EventHandlingStrategy =
        BlockAppendedEventHandlingStrategy(
            handleRollbackFactory = handleRollbackFactory,
            appendedBlockHistoryBuffer = appendedBlockHistoryBuffer,
        )
}
