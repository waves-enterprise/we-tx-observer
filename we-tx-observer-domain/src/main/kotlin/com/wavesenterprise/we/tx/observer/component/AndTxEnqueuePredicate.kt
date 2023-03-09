package com.wavesenterprise.we.tx.observer.component

import com.wavesenterprise.sdk.node.domain.tx.Tx

class AndTxEnqueuePredicate(
    private val predicates: List<TxEnqueuePredicate>
) : TxEnqueuePredicate {
    override fun isEnqueued(tx: Tx): Boolean =
        predicates.all {
            it.isEnqueued(tx)
        }
}
