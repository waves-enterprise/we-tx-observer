package com.wavesenterprise.we.tx.observer.starter

import com.fasterxml.jackson.databind.ObjectMapper
import com.wavesenterprise.sdk.node.domain.TxType
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.we.tx.observer.api.partition.TxQueuePartitionResolver
import com.wavesenterprise.we.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.we.tx.observer.api.tx.TxEnqueuePredicate

/**
 * Configurer of the main elements of the observer.
 */
interface TxObserverConfigurer {

    /**
     * Transaction predicates configurer (builder).
     * Provides the ability to add predicates to filter transactions in different ways.
     * @see TxEnqueuePredicate
     */
    interface TxEnqueuePredicateConfigurer {

        /**
         * Add a predicate
         * @param predicate TxEnqueuePredicate
         */
        fun predicate(predicate: TxEnqueuePredicate): TxEnqueuePredicateConfigurer

        /**
         * Add a predicate as a lambda expression
         * @param predicate lambda expression
         */
        fun predicate(predicate: (Tx) -> Boolean): TxEnqueuePredicateConfigurer

        /**
         * Add predicates from a sequence of transaction types
         * @param types
         */
        fun types(types: Iterable<TxType>): TxEnqueuePredicateConfigurer

        /**
         * Add predicates as a variable number of transaction types (vararg)
         * @param types
         */
        fun types(vararg types: TxType): TxEnqueuePredicateConfigurer
    }

    /**
     * Specify partition resolver
     * @see TxQueuePartitionResolver
     */
    fun partitionResolver(): TxQueuePartitionResolver?

    /**
     * Configure predicates
     * @param predicateConfigurer
     */
    fun configure(predicateConfigurer: TxEnqueuePredicateConfigurer)

    /**
     * Specify partition private content resolver
     * @see TxQueuePartitionResolver
     */
    fun privateContentResolver(): PrivateContentResolver?

    /**
     * Specify object mapper
     */
    fun objectMapper(): ObjectMapper?
}
