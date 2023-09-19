package com.wavesenterprise.we.tx.observer.starter

import com.wavesenterprise.we.tx.observer.api.tx.TxEnqueuePredicate

interface TxEnqueuePredicatesSupplier {
    fun predicates(): List<TxEnqueuePredicate>
}
