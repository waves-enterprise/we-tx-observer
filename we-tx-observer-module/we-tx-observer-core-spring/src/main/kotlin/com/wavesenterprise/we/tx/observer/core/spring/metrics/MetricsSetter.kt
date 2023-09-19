package com.wavesenterprise.we.tx.observer.core.spring.metrics

import com.wavesenterprise.we.tx.observer.core.spring.executor.PREFIX
import io.micrometer.core.instrument.MeterRegistry

class MetricsSetter(
    val meterRegistry: MeterRegistry,
    val metrics: List<MetricsContainer>,
) {
    init {
        metrics.forEach {
            meterRegistry.registerMetric(it)
        }
    }
}

fun MeterRegistry.registerMetric(metricsContainer: MetricsContainer) {
    gauge(PREFIX + metricsContainer.metricName, metricsContainer) {
        metricsContainer.metricValue.toDouble()
    }
}
