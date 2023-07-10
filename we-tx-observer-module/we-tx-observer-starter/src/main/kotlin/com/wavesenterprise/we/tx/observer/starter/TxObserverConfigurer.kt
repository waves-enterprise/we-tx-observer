package com.wavesenterprise.we.tx.observer.starter

import com.fasterxml.jackson.databind.ObjectMapper
import com.wavesenterprise.sdk.node.domain.TxType
import com.wavesenterprise.we.tx.observer.api.partition.TxQueuePartitionResolver
import com.wavesenterprise.we.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.we.tx.observer.api.tx.TxEnqueuePredicate

interface TxObserverConfigurer {
    interface TxEnqueuePredicateConfigurer {
        fun predicate(predicate: TxEnqueuePredicate): TxEnqueuePredicateConfigurer
        fun types(types: Iterable<TxType>): TxEnqueuePredicateConfigurer
        fun types(vararg types: TxType): TxEnqueuePredicateConfigurer
    }

    fun partitionResolver(): TxQueuePartitionResolver?

    fun configure(predicateConfigurer: TxEnqueuePredicateConfigurer)

    fun privateContentResolver(): PrivateContentResolver?

    fun objectMapper(): ObjectMapper?
}
