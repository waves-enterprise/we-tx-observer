package com.wavesenterprise.we.tx.observer.starter

import com.wavesenterprise.sdk.node.domain.TxType
import com.wavesenterprise.we.tx.observer.api.block.subscriber.BlockSubscriber
import com.wavesenterprise.we.tx.observer.api.block.subscriber.RollbackSubscriber
import com.wavesenterprise.we.tx.observer.api.tx.TxEnqueuePredicate
import com.wavesenterprise.we.tx.observer.core.spring.component.CachingTxEnqueuePredicate
import com.wavesenterprise.we.tx.observer.core.spring.component.LoggingBlockSubscriber
import com.wavesenterprise.we.tx.observer.core.spring.component.TxTypeEnqueuedPredicate
import com.wavesenterprise.we.tx.observer.core.spring.executor.EnqueueingBlockSubscriber
import com.wavesenterprise.we.tx.observer.core.spring.executor.FILTERED_TX_COUNT
import com.wavesenterprise.we.tx.observer.core.spring.executor.MetricRollbackSubscriber
import com.wavesenterprise.we.tx.observer.core.spring.executor.PersistingRollbackSubscriber
import com.wavesenterprise.we.tx.observer.core.spring.executor.ROLLBACK_COUNT
import com.wavesenterprise.we.tx.observer.core.spring.executor.TOTAL_LOGICAL_TX_COUNT
import com.wavesenterprise.we.tx.observer.core.spring.executor.TOTAL_TX_COUNT
import com.wavesenterprise.we.tx.observer.core.spring.metrics.AddableLongMetricsContainer
import com.wavesenterprise.we.tx.observer.core.spring.metrics.MetricContainerData
import com.wavesenterprise.we.tx.observer.core.spring.partition.TxQueuePartitionResolveService
import com.wavesenterprise.we.tx.observer.core.spring.web.service.RollbackInfoService
import com.wavesenterprise.we.tx.observer.core.spring.web.service.RollbackInfoServiceImpl
import com.wavesenterprise.we.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.we.tx.observer.jpa.repository.RollbackInfoRepository
import com.wavesenterprise.we.tx.observer.starter.executor.poller.PollerBlockSourceConfiguration
import com.wavesenterprise.we.tx.observer.starter.executor.subscriber.SubscriberBlockSourceConfiguration
import com.wavesenterprise.we.tx.observer.starter.executor.syncinfo.SyncInfoConfig
import com.wavesenterprise.we.tx.observer.starter.properties.TxEnqueuedPredicateProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    TxObserverJpaConfig::class,
    SyncInfoConfig::class,
    PollerBlockSourceConfiguration::class,
    SubscriberBlockSourceConfiguration::class
)
@EnableConfigurationProperties(
    TxEnqueuedPredicateProperties::class
)
@SuppressWarnings("TooManyFunctions")
class BlockInfoSynchronizerConfig(
    private val enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
    private val txQueuePartitionResolveService: TxQueuePartitionResolveService,
    private val rollbackInfoRepository: RollbackInfoRepository,
) {
    @Bean
    @ConditionalOnExpression("!'\${tx-observer.predicate.tx-types[0]:}'.isEmpty()")
    fun txTypeEnqueuedPredicate(
        txEnqueuedPredicateProperties: TxEnqueuedPredicateProperties,
    ): TxTypeEnqueuedPredicate =
        TxTypeEnqueuedPredicate(txEnqueuedPredicateProperties.txTypes.map { TxType.fromInt(it) })

    @Bean
    @ConditionalOnMissingBean
    fun txEnqueuePredicatesSupplier(
        predicates: List<TxEnqueuePredicate>,
    ): TxEnqueuePredicatesSupplier =
        object : TxEnqueuePredicatesSupplier {
            override fun predicates(): List<TxEnqueuePredicate> =
                predicates
        }

    @Bean
    fun enqueueingBlockSubscriber(
        predicatesSupplier: TxEnqueuePredicatesSupplier,
        txEnqueuedPredicateProperties: TxEnqueuedPredicateProperties,
        totalTxMetricsContainer: AddableLongMetricsContainer,
        filteredTxMetricsContainer: AddableLongMetricsContainer,
        totalLogicalTxMetricsContainer: AddableLongMetricsContainer,
    ): BlockSubscriber =
        EnqueueingBlockSubscriber(
            enqueuedTxJpaRepository = enqueuedTxJpaRepository,
            txQueuePartitionResolveService = txQueuePartitionResolveService,
            txEnqueuePredicate = CachingTxEnqueuePredicate(
                predicates = predicatesSupplier.predicates(),
                cacheDuration = txEnqueuedPredicateProperties.cacheTtl,
            ),
            totalTxMetricsContainer = totalTxMetricsContainer,
            filteredTxMetricContainer = filteredTxMetricsContainer,
            totalLogicalTxMetricsContainer = totalLogicalTxMetricsContainer,
        )

    @Bean
    fun loggingBlockSubscriber(): BlockSubscriber = LoggingBlockSubscriber()

    @Bean
    fun totalTxMetricsContainer(): AddableLongMetricsContainer =
        MetricContainerData(metricName = TOTAL_TX_COUNT)

    @Bean
    fun totalLogicalTxMetricsContainer(): AddableLongMetricsContainer =
        MetricContainerData(metricName = TOTAL_LOGICAL_TX_COUNT)

    @Bean
    fun filteredTxMetricsContainer(): AddableLongMetricsContainer = MetricContainerData(
        metricName = FILTERED_TX_COUNT,
    )

    @Bean
    fun rollbackInfoService(): RollbackInfoService =
        RollbackInfoServiceImpl(
            rollbackInfoRepository = rollbackInfoRepository,
        )

    @Bean
    fun persistingRollbackSubscriber(): RollbackSubscriber =
        PersistingRollbackSubscriber(
            rollbackInfoRepository = rollbackInfoRepository,
        )

    @Bean
    fun metricRollbackSubscriber(
        rollbackCountMetricsContainer: AddableLongMetricsContainer,
    ): RollbackSubscriber =
        MetricRollbackSubscriber(
            rollbackCountMetricsContainer = rollbackCountMetricsContainer,
        )

    @Bean
    fun rollbackCountMetricsContainer(): AddableLongMetricsContainer = MetricContainerData(
        metricName = ROLLBACK_COUNT,
    )
}
