package com.wavesenterprise.we.tx.observer.core.spring.component

import com.wavesenterprise.sdk.node.domain.TxType
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.domain.tx.Tx.Companion.type
import com.wavesenterprise.we.tx.observer.api.tx.TxEnqueuePredicate

class TxTypeEnqueuedPredicate(
    val txTypes: List<TxType>,
) : TxEnqueuePredicate {

    override fun isEnqueued(tx: Tx): Boolean =
        txTypes.let {
            it.isEmpty() || it.contains(tx.type())
        }
}
