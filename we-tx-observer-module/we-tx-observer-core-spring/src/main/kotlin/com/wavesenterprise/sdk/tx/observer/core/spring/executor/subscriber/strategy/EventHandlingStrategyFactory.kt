package com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber.strategy

interface EventHandlingStrategyFactory {
    fun create(height: Long): EventHandlingStrategy
}
