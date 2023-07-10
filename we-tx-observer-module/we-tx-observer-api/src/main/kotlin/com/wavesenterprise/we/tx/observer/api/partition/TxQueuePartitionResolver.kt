package com.wavesenterprise.we.tx.observer.api.partition

import com.wavesenterprise.sdk.node.domain.tx.Tx

interface TxQueuePartitionResolver {

    fun resolvePartitionId(tx: Tx): String?
}
