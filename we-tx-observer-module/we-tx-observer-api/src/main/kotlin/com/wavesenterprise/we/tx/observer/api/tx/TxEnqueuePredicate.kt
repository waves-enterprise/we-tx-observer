package com.wavesenterprise.we.tx.observer.api.tx

import com.wavesenterprise.sdk.node.domain.tx.Tx

interface TxEnqueuePredicate {

    fun isEnqueued(tx: Tx): Boolean
}
