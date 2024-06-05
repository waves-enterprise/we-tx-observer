package com.wavesenterprise.sdk.tx.observer.core.spring.metrics

interface AddableLongMetricsContainer : MetricsContainer {
    fun add(delta: Long)
}
