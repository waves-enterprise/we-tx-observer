package com.wavesenterprise.sdk.tx.observer.core.spring.executor.poller

interface PositionedSourceExecutor {
    fun syncToBlockHeight(newBlockHeightPosition: Long): Long
}
