package com.wavesenterprise.sdk.tx.observer.core.spring.partition

import com.wavesenterprise.sdk.tx.observer.core.spring.metrics.AddableLongMetricsContainer

class MetricPartitionHandler(
    private val partitionHandler: PartitionHandler,
    private val processedTxMetricsContainer: AddableLongMetricsContainer,
    private val partitionFailureMetricsContainer: AddableLongMetricsContainer,
) : PartitionHandler by partitionHandler {
    override fun handleErrorWhenReading(partitionId: String) {
        partitionFailureMetricsContainer.add(1)
        partitionHandler.handleErrorWhenReading(partitionId)
    }

    override fun handleSuccessWhenReading(partitionId: String, txCount: Long) {
        processedTxMetricsContainer.add(txCount)
        partitionHandler.handleSuccessWhenReading(partitionId, txCount)
    }
}
