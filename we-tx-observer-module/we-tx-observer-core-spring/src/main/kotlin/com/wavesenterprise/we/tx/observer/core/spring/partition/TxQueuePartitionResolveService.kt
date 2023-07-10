package com.wavesenterprise.we.tx.observer.core.spring.partition

import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.we.tx.observer.domain.TxQueuePartition

interface TxQueuePartitionResolveService {

    fun resolveTxQueuePartition(tx: Tx, resetLastReadTxId: Boolean = false): TxQueuePartition
}
