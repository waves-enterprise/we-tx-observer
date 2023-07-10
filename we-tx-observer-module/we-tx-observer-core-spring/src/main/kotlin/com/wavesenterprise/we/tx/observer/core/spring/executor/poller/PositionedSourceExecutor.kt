package com.wavesenterprise.we.tx.observer.core.spring.executor.poller

interface PositionedSourceExecutor {
    fun syncToBlockHeight(newBlockHeightPosition: Long): Long
}
