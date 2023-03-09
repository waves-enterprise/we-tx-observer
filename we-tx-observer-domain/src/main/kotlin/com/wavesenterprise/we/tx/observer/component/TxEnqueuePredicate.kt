package com.wavesenterprise.we.tx.observer.component

import com.wavesenterprise.sdk.node.domain.tx.Tx

interface TxEnqueuePredicate {

    fun isEnqueued(tx: Tx): Boolean
}
