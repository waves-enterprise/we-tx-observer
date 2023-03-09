package com.wavesenterprise.we.tx.observer.component

import com.wavesenterprise.sdk.node.domain.tx.Tx

interface TxQueuePartitionResolver {

    fun resolvePartitionId(tx: Tx): String?
}
