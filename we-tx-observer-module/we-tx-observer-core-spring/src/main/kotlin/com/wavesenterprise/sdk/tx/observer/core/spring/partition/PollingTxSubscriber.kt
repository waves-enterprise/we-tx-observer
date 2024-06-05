package com.wavesenterprise.sdk.tx.observer.core.spring.partition

interface PollingTxSubscriber {

    fun dequeuePartitionAndSendToSubscribers(partitionId: String): Int
}
