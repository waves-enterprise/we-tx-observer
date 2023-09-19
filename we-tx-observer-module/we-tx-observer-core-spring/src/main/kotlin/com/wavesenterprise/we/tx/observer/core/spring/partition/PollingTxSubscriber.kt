package com.wavesenterprise.we.tx.observer.core.spring.partition

interface PollingTxSubscriber {

    fun dequeuePartitionAndSendToSubscribers(partitionId: String): Int
}
