package com.wavesenterprise.we.tx.observer.core.spring.partition

interface LatestTxPartitionPoller {
    fun pollLatestActualPartition(): String?
}
