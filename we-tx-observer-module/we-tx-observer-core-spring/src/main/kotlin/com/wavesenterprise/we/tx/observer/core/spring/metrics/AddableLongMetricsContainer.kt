package com.wavesenterprise.we.tx.observer.core.spring.metrics

interface AddableLongMetricsContainer : MetricsContainer {
    fun add(delta: Long)
}
