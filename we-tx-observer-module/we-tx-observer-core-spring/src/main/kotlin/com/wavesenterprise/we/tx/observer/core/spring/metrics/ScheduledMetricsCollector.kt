package com.wavesenterprise.we.tx.observer.core.spring.metrics

import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.we.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class ScheduledMetricsCollector(
    val enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
    val txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository,
    val queueSizeMetric: MetricsContainer,
    val queueHeightMetric: MetricsContainer,
    val nodeHeightMetric: MetricsContainer,
    val notAvailablePrivacyMetric: MetricsContainer,
    val errorPartitionCountMetric: MetricsContainer,
    val totalPartitionCountMetric: MetricsContainer,
) {

    val logger: Logger = LoggerFactory.getLogger(ScheduledMetricsCollector::class.java)

    open fun metricsCollector() {
        logger.debug("Started collecting tx-observer metrics")
        val queueHeight = enqueuedTxJpaRepository.findMinHeightForStatus(EnqueuedTxStatus.NEW)
            ?: nodeHeightMetric.metricValue
        val queueSize = enqueuedTxJpaRepository.countByStatus(EnqueuedTxStatus.NEW)
        queueSizeMetric.metricValue = queueSize
        queueHeightMetric.metricValue = queueHeight
        notAvailablePrivacyMetric.metricValue = enqueuedTxJpaRepository.countNotAvailablePolicyDataHashes()
        errorPartitionCountMetric.metricValue = txQueuePartitionJpaRepository.countErrorPartitions()
        totalPartitionCountMetric.metricValue = txQueuePartitionJpaRepository.count()
        logger.debug("Finished collecting tx-observer metrics")
    }
}
