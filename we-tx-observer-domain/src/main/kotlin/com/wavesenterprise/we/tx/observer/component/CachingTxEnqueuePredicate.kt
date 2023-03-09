package com.wavesenterprise.we.tx.observer.component

import com.github.benmanes.caffeine.cache.Caffeine
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.tx.Tx
import java.time.Duration

class CachingTxEnqueuePredicate(
    val predicates: List<TxEnqueuePredicate>,
    val cacheTtl: Duration
) : TxEnqueuePredicate {

    private val predicateTtlCache = Caffeine.newBuilder()
        .expireAfterWrite(cacheTtl)
        .build<TxId, Boolean>()
        .asMap()

    override fun isEnqueued(tx: Tx): Boolean =
        predicateTtlCache.computeIfAbsent(tx.id) { predicates.all { it.isEnqueued(tx) } }
}
