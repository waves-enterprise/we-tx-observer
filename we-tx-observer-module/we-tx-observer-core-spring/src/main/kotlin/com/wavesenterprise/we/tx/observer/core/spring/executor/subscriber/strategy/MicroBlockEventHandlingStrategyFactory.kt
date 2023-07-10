package com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.strategy

import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.HandleRollbackFactory

class MicroBlockEventHandlingStrategyFactory(
    private val handleRollbackFactory: HandleRollbackFactory,
    private val appendedBlockHistoryBuffer: AppendedBlockHistoryBuffer,
) : EventHandlingStrategyFactory {
    override fun create(height: Long): EventHandlingStrategy =
        MicroBlockEventHandlingStrategy(
            handleRollbackFactory = handleRollbackFactory,
            appendedBlockHistoryBuffer = appendedBlockHistoryBuffer,
            height = height,
        )
}
