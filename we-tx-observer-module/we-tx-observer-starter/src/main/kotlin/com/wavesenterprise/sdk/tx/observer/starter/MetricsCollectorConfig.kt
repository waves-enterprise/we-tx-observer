package com.wavesenterprise.sdk.tx.observer.starter

import com.wavesenterprise.sdk.tx.observer.core.spring.executor.ERROR_PARTITION_COUNT
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.QUEUE_HEIGHT
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.QUEUE_SIZE
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.TOTAL_PARTITION_COUNT
import com.wavesenterprise.sdk.tx.observer.core.spring.metrics.MetricContainerData
import com.wavesenterprise.sdk.tx.observer.core.spring.metrics.MetricsContainer
import com.wavesenterprise.sdk.tx.observer.core.spring.metrics.ScheduledMetricsCollector
import com.wavesenterprise.sdk.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.sdk.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.sdk.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.sdk.tx.observer.starter.properties.MetricsCollectorProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    TxObserverJpaConfig::class,
    BlockInfoSynchronizerConfig::class,
)
@EnableConfigurationProperties(MetricsCollectorProperties::class)
class MetricsCollectorConfig {

    @Bean
    fun queueSizeMetric() = MetricContainerData(metricName = QUEUE_SIZE)

    @Bean
    fun queueHeightMetric() = MetricContainerData(metricName = QUEUE_HEIGHT)

    @Bean
    fun errorPartitionCountMetric() =
        MetricContainerData(metricName = ERROR_PARTITION_COUNT)

    @Bean
    fun totalPartitionCountMetric() =
        MetricContainerData(metricName = TOTAL_PARTITION_COUNT)

    @Suppress("LongParameterList")
    @Bean
    fun metricsCollector(
        enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
        txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository,
        queueSizeMetric: MetricsContainer,
        queueHeightMetric: MetricsContainer,
        nodeHeightMetric: MetricsContainer,
        notAvailablePrivacyMetric: MetricsContainer,
        errorPartitionCountMetric: MetricsContainer,
        totalPartitionCountMetric: MetricsContainer,
    ) = ScheduledMetricsCollector(
        enqueuedTxJpaRepository = enqueuedTxJpaRepository,
        txQueuePartitionJpaRepository = txQueuePartitionJpaRepository,
        queueSizeMetric = queueSizeMetric,
        queueHeightMetric = queueHeightMetric,
        nodeHeightMetric = nodeHeightMetric,
        notAvailablePrivacyMetric = notAvailablePrivacyMetric,
        errorPartitionCountMetric = errorPartitionCountMetric,
        totalPartitionCountMetric = totalPartitionCountMetric,
    )
}
