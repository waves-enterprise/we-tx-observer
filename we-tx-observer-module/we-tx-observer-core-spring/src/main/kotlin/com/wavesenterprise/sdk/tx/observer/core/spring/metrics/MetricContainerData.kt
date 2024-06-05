package com.wavesenterprise.sdk.tx.observer.core.spring.metrics

import java.util.concurrent.atomic.AtomicLong

data class MetricContainerData(
    override val metricName: String,
) : AddableLongMetricsContainer {

    private var atomicValue: AtomicLong = AtomicLong(0)

    override var metricValue: Number
        get() = atomicValue.get()
        set(value) {
            atomicValue.set(value.toLong())
        }

    override fun add(delta: Long) {
        atomicValue.addAndGet(delta)
    }
}
