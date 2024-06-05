package com.wavesenterprise.sdk.tx.observer.starter

import com.wavesenterprise.sdk.tx.observer.api.tx.TxEnqueuePredicate

interface TxEnqueuePredicatesSupplier {
    fun predicates(): List<TxEnqueuePredicate>
}
