package com.wavesenterprise.we.tx.observer.core.spring.executor.poller

interface SourceExecutor {
    fun execute(blockHeightLowerBound: Long, blockHeightUpperBound: Long): Long
}
