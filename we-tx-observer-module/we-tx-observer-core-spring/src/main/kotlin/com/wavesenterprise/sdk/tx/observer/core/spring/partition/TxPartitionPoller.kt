package com.wavesenterprise.sdk.tx.observer.core.spring.partition

interface TxPartitionPoller {
    fun pollPartition(): String?
}
