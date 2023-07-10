package com.wavesenterprise.we.tx.observer.starter

import com.fasterxml.jackson.databind.ObjectMapper
import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.node.domain.TxType
import com.wavesenterprise.we.tx.observer.api.partition.TxQueuePartitionResolver
import com.wavesenterprise.we.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.we.tx.observer.api.tx.TxEnqueuePredicate
import com.wavesenterprise.we.tx.observer.core.spring.component.AndTxEnqueuePredicate
import com.wavesenterprise.we.tx.observer.core.spring.component.TxTypeEnqueuedPredicate
import com.wavesenterprise.we.tx.observer.core.spring.method.callback.PrivateContentResolverImpl
import com.wavesenterprise.we.tx.observer.core.spring.partition.DefaultPartitionResolver
import com.wavesenterprise.we.tx.observer.starter.properties.TxEnqueuedPredicateProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@EnableConfigurationProperties(TxEnqueuedPredicateProperties::class)
class TxObserverEnablerConfig {
    private class TxEnqueuePredicateConfigurerImpl : TxObserverConfigurer.TxEnqueuePredicateConfigurer {
        private val txEnqueuePredicates: MutableList<TxEnqueuePredicate> = mutableListOf()
        private val txTypes: MutableSet<TxType> = mutableSetOf()

        val predicates: List<TxEnqueuePredicate>
            get() = txEnqueuePredicates

        val types: List<TxType>
            get() = txTypes.toList()

        override fun predicate(predicate: TxEnqueuePredicate): TxObserverConfigurer.TxEnqueuePredicateConfigurer =
            apply {
                txEnqueuePredicates.add(predicate)
            }

        override fun types(types: Iterable<TxType>): TxObserverConfigurer.TxEnqueuePredicateConfigurer =
            apply {
                txTypes.addAll(types)
            }

        override fun types(vararg types: TxType): TxObserverConfigurer.TxEnqueuePredicateConfigurer =
            types(types.toSet())
    }

    @Bean
    fun txEnqueuePredicatesSupplier(
        txEnqueuePredicate: TxEnqueuePredicate,
    ): TxEnqueuePredicatesSupplier =
        object : TxEnqueuePredicatesSupplier {
            override fun predicates(): List<TxEnqueuePredicate> =
                listOf(txEnqueuePredicate)
        }

    @Bean
    @Primary
    fun txEnqueuePredicate(
        @Autowired(required = false) txObserverConfigurer: TxObserverConfigurer?,
        txEnqueuedPredicateProperties: TxEnqueuedPredicateProperties,
    ): TxEnqueuePredicate =
        TxEnqueuePredicateConfigurerImpl()
            .apply {
                txObserverConfigurer?.configure(this)
            }
            .let { predicateConfigurer ->
                val predicates = predicateConfigurer.predicates
                val types = predicateConfigurer.types
                AndTxEnqueuePredicate(
                    predicates + typeEnqueuedPredicate(types, txEnqueuedPredicateProperties)
                )
            }

    private fun typeEnqueuedPredicate(
        configuredTypes: List<TxType>,
        txEnqueuedPredicateProperties: TxEnqueuedPredicateProperties,
    ) = when {
        configuredTypes.isNotEmpty() -> {
            listOf(
                TxTypeEnqueuedPredicate(configuredTypes)
            )
        }
        txEnqueuedPredicateProperties.txTypes.isNotEmpty() -> listOf(
            TxTypeEnqueuedPredicate(
                txEnqueuedPredicateProperties.txTypes.map { TxType.fromInt(it) }
            )
        )
        else -> emptyList()
    }

    @Bean
    @Primary
    fun txObserverConfigurerPartitionResolver(
        @Autowired(required = false) txObserverConfigurer: TxObserverConfigurer?,
    ): TxQueuePartitionResolver =
        txObserverConfigurer?.partitionResolver() ?: DefaultPartitionResolver()

    @Bean
    @Primary
    @ConditionalOnBean(NodeBlockingServiceFactory::class)
    fun txObserverConfigurerPrivateContentResolver(
        @Autowired(required = false) txObserverConfigurer: TxObserverConfigurer?,
        nodeBlockingServiceFactory: NodeBlockingServiceFactory,
        @Autowired(required = false) objectMapper: ObjectMapper?,
    ): PrivateContentResolver =
        txObserverConfigurer?.privateContentResolver() ?: PrivateContentResolverImpl(
            nodeBlockingServiceFactory = nodeBlockingServiceFactory,
            objectMapper = txObserverConfigurer?.objectMapper()
                ?: objectMapper
                ?: error("Object mapper must be configured or be present in spring context")
        )
}
