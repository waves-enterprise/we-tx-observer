package com.wavesenterprise.we.tx.observer.starter

import com.fasterxml.jackson.databind.ObjectMapper
import com.wavesenterprise.sdk.node.domain.TxType
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.we.tx.observer.api.partition.TxQueuePartitionResolver
import com.wavesenterprise.we.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.we.tx.observer.api.tx.TxEnqueuePredicate

class TxObserverConfigurerBuilder {
    var privateContentResolver: PrivateContentResolver? = null
    var objectMapper: ObjectMapper? = null
    var partitionResolver: TxQueuePartitionResolver? = null

    val txEnqueuePredicatesHolder: TxEnqueuePredicatesHolder = TxEnqueuePredicatesHolder()

    fun types(vararg types: TxType): TxObserverConfigurerBuilder =
        this.apply {
            txEnqueuePredicatesHolder.types(types.toSet())
        }

    fun types(types: Iterable<TxType>) =
        this.apply {
            txEnqueuePredicatesHolder.types(types)
        }

    fun predicate(predicate: (Tx) -> Boolean) =
        this.apply {
            txEnqueuePredicatesHolder.predicate(predicate)
        }

    fun predicate(predicate: TxEnqueuePredicate) =
        this.apply {
            txEnqueuePredicatesHolder.predicate(predicate)
        }

    fun partitionResolver(partitionResolver: TxQueuePartitionResolver): TxObserverConfigurerBuilder =
        apply { this.partitionResolver = partitionResolver }

    fun privateContentResolver(privateContentResolver: PrivateContentResolver): TxObserverConfigurerBuilder =
        apply { this.privateContentResolver = privateContentResolver }

    fun objectMapper(objectMapper: ObjectMapper): TxObserverConfigurerBuilder =
        apply { this.objectMapper = objectMapper }

    fun build(): TxObserverConfigurer {
        return object : TxObserverConfigurer {

            override fun partitionResolver(): TxQueuePartitionResolver? = partitionResolver

            override fun configure(predicateConfigurer: TxObserverConfigurer.TxEnqueuePredicateConfigurer) {
                val predicates = txEnqueuePredicatesHolder.predicates
                val types = txEnqueuePredicatesHolder.types
                predicates.forEach { predicateConfigurer.predicate(it) }
                predicateConfigurer.types(types)
            }

            override fun privateContentResolver(): PrivateContentResolver? = privateContentResolver

            override fun objectMapper(): ObjectMapper? = objectMapper
        }
    }

    class TxEnqueuePredicatesHolder(
        private val txEnqueuePredicates: MutableList<TxEnqueuePredicate> = mutableListOf(),
        private val txTypes: MutableSet<TxType> = mutableSetOf()
    ) {
        val predicates: List<TxEnqueuePredicate>
            get() = txEnqueuePredicates
        val types: List<TxType>
            get() = txTypes.toList()

        fun predicate(predicate: TxEnqueuePredicate) {
            txEnqueuePredicates.add(predicate)
        }

        fun predicate(predicate: (Tx) -> Boolean) {
            txEnqueuePredicates.add(
                object : TxEnqueuePredicate {
                    override fun isEnqueued(tx: Tx): Boolean =
                        predicate(tx)
                }
            )
        }

        fun types(types: Iterable<TxType>) {
            txTypes.addAll(types)
        }

        fun types(vararg types: TxType) {
            txTypes.addAll(types.toSet())
        }
    }
}
