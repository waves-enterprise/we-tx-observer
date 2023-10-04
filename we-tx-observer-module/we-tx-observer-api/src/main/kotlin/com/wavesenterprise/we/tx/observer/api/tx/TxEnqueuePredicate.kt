package com.wavesenterprise.we.tx.observer.api.tx

import com.wavesenterprise.sdk.node.domain.tx.Tx

/**
 * Predicate for filtering transactions.
 * It is designed to filter transactions that appear in the persistent queue for future handling.
 * This is an interface with a single isEnqueued(Tx) method.
 */
interface TxEnqueuePredicate {

    fun isEnqueued(tx: Tx): Boolean
}
