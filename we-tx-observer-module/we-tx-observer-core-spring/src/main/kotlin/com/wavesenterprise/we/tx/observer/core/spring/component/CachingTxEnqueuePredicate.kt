package com.wavesenterprise.we.tx.observer.core.spring.component

import com.github.benmanes.caffeine.cache.Caffeine
import com.wavesenterprise.sdk.node.client.blocking.cache.CaffeineLoadingCache
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.we.tx.observer.api.tx.TxEnqueuePredicate
import java.time.Duration

class CachingTxEnqueuePredicate(
    val predicates: List<TxEnqueuePredicate>,
    val cacheDuration: Duration,
) : TxEnqueuePredicate {

    private val predicateTtlCache = CaffeineLoadingCache<TxId, Boolean>(
        Caffeine.newBuilder()
            .expireAfterWrite(cacheDuration)
            .build()
    )

    override fun isEnqueued(tx: Tx): Boolean =
        predicateTtlCache.loadNotNull(tx.id) { predicates.all { it.isEnqueued(tx) } }
}
