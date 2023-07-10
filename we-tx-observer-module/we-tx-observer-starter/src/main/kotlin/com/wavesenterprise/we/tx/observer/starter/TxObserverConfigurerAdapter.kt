package com.wavesenterprise.we.tx.observer.starter

import com.fasterxml.jackson.databind.ObjectMapper
import com.wavesenterprise.we.tx.observer.api.partition.TxQueuePartitionResolver
import com.wavesenterprise.we.tx.observer.api.privacy.PrivateContentResolver

open class TxObserverConfigurerAdapter : TxObserverConfigurer {
    override fun partitionResolver(): TxQueuePartitionResolver? = null

    override fun configure(predicateConfigurer: TxObserverConfigurer.TxEnqueuePredicateConfigurer) {}

    override fun privateContentResolver(): PrivateContentResolver? = null

    override fun objectMapper(): ObjectMapper? = null
}
