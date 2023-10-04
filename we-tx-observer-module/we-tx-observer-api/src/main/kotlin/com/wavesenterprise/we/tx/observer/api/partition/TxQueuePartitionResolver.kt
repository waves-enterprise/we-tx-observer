package com.wavesenterprise.we.tx.observer.api.partition

import com.wavesenterprise.sdk.node.domain.tx.Tx

/**
 * Specifies the partition for the received transaction (defaultPartitionId by default)
 */
interface TxQueuePartitionResolver {

    /**
     * Method for determining the partition for incoming transactions.
     * @param tx
     * @return partitionId in which the transaction will be placed or null to place it in the default partition
     */
    fun resolvePartitionId(tx: Tx): String?
}
