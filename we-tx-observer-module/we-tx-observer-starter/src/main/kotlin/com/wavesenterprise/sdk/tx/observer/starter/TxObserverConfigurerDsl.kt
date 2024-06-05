package com.wavesenterprise.sdk.tx.observer.starter

import com.fasterxml.jackson.databind.ObjectMapper
import com.wavesenterprise.sdk.node.domain.TxType
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.tx.observer.api.partition.TxQueuePartitionResolver
import com.wavesenterprise.sdk.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.sdk.tx.observer.api.tx.TxEnqueuePredicate

fun observerConfigurer(init: TxObserverConfigurerContext.() -> Unit = {}): TxObserverConfigurer =
    TxObserverConfigurerContextImpl()
        .apply(init)
        .build()

@DslMarker
annotation class ConfigurerDslMarker

@ConfigurerDslMarker
interface TxObserverConfigurerContext {
    var partitionResolver: TxQueuePartitionResolver?
    var privateContentResolver: PrivateContentResolver?
    var objectMapper: ObjectMapper?
    fun predicates(init: TxEnqueuePredicateContext.() -> Unit)
}

@ConfigurerDslMarker
interface TxEnqueuePredicateContext {
    fun predicate(predicate: TxEnqueuePredicate)
    fun predicate(predicate: (Tx) -> Boolean)
    fun types(types: Iterable<Int>)
    fun types(vararg types: Int)
}

internal class TxObserverConfigurerContextImpl : TxObserverConfigurerContext {
    override var partitionResolver: TxQueuePartitionResolver? = null
    override var privateContentResolver: PrivateContentResolver? = null
    override var objectMapper: ObjectMapper? = null
    private val txEnqueuePredicateConfigurerContext: TxEnqueuePredicateContextImpl = TxEnqueuePredicateContextImpl()

    override fun predicates(init: (TxEnqueuePredicateContext) -> Unit) =
        init(txEnqueuePredicateConfigurerContext)

    fun build(): TxObserverConfigurer = object : TxObserverConfigurer {
        override fun partitionResolver(): TxQueuePartitionResolver? = partitionResolver

        override fun configure(predicateConfigurer: TxObserverConfigurer.TxEnqueuePredicateConfigurer) {
            val predicates = txEnqueuePredicateConfigurerContext.predicates
            val types = txEnqueuePredicateConfigurerContext.types
            predicates.forEach { predicateConfigurer.predicate(it) }
            predicateConfigurer.types(types.map { TxType.fromInt(it) })
        }

        override fun privateContentResolver(): PrivateContentResolver? = privateContentResolver

        override fun objectMapper(): ObjectMapper? = objectMapper
    }
}

internal class TxEnqueuePredicateContextImpl : TxEnqueuePredicateContext {
    private val txEnqueuePredicates: MutableList<TxEnqueuePredicate> = mutableListOf()
    private val txTypes: MutableSet<Int> = mutableSetOf()

    val predicates: List<TxEnqueuePredicate>
        get() = txEnqueuePredicates
    val types: List<Int>
        get() = txTypes.toList()

    override fun predicate(predicate: TxEnqueuePredicate) {
        txEnqueuePredicates.add(predicate)
    }

    override fun predicate(predicate: (Tx) -> Boolean) {
        txEnqueuePredicates.add(
            object : TxEnqueuePredicate {
                override fun isEnqueued(tx: Tx): Boolean =
                    predicate(tx)
            }
        )
    }

    override fun types(types: Iterable<Int>) {
        txTypes.addAll(types)
    }

    override fun types(vararg types: Int) {
        txTypes.addAll(types.toSet())
    }
}
