package com.wavesenterprise.sdk.tx.observer.starter

import com.wavesenterprise.sdk.tx.observer.core.spring.executor.HANDLED_TX_COUNT
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.PARTITION_HANDLING_FAILURE_COUNT
import com.wavesenterprise.sdk.tx.observer.core.spring.metrics.AddableLongMetricsContainer
import com.wavesenterprise.sdk.tx.observer.core.spring.metrics.MetricContainerData
import com.wavesenterprise.sdk.tx.observer.core.spring.partition.MetricPartitionHandler
import com.wavesenterprise.sdk.tx.observer.core.spring.partition.PartitionHandler
import com.wavesenterprise.sdk.tx.observer.core.spring.partition.PartitionHandlerJpa
import com.wavesenterprise.sdk.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.sdk.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.sdk.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary

@Configuration
@Import(TxObserverJpaConfig::class)
@ConditionalOnMissingBean(PartitionHandler::class)
class PartitionHandlerConfig {
    @Bean
    fun handledTxMetricsContainer(): AddableLongMetricsContainer = MetricContainerData(
        metricName = HANDLED_TX_COUNT,
    )

    @Bean
    fun partitionFailureMetricsContainer(): AddableLongMetricsContainer = MetricContainerData(
        metricName = PARTITION_HANDLING_FAILURE_COUNT,
    )

    @Bean
    @Primary
    fun partitionHandler(
        @Qualifier("partitionHandlerJpa") partitionHandlerJpa: PartitionHandler,
        handledTxMetricsContainer: AddableLongMetricsContainer,
        partitionFailureMetricsContainer: AddableLongMetricsContainer,
    ): PartitionHandler = MetricPartitionHandler(
        partitionHandlerJpa,
        handledTxMetricsContainer,
        partitionFailureMetricsContainer
    )

    @Bean
    fun partitionHandlerJpa(
        txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository,
        enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
    ): PartitionHandler = PartitionHandlerJpa(
        partitionJpaRepository = txQueuePartitionJpaRepository,
        enqueuedTxJpaRepository = enqueuedTxJpaRepository,
    )
}
