package com.wavesenterprise.sdk.tx.observer.core.spring.partition

import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.tx.observer.api.partition.TxQueuePartitionResolver

class DefaultPartitionResolver : TxQueuePartitionResolver {

    override fun resolvePartitionId(tx: Tx): String? = null
}
