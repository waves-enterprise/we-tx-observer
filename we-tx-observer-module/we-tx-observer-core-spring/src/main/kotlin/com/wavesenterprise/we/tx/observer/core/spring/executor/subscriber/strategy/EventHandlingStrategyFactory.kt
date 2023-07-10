package com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.strategy

interface EventHandlingStrategyFactory {
    fun create(height: Long): EventHandlingStrategy
}
