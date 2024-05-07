package com.wavesenterprise.we.tx.observer.core.spring.partition

interface TxPartitionPoller {
    fun pollPartition(): String?
}
