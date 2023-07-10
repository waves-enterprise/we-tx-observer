package com.wavesenterprise.we.tx.observer.core.spring.component

import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.we.tx.observer.api.tx.TxEnqueuePredicate

class AndTxEnqueuePredicate(
    private val predicates: List<TxEnqueuePredicate>,
) : TxEnqueuePredicate {
    override fun isEnqueued(tx: Tx): Boolean =
        predicates.all {
            it.isEnqueued(tx)
        }
}
