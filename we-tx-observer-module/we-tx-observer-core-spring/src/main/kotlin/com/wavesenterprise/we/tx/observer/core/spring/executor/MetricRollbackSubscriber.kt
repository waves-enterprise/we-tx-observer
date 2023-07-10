package com.wavesenterprise.we.tx.observer.core.spring.executor

import com.wavesenterprise.we.tx.observer.api.block.WeRollbackInfo
import com.wavesenterprise.we.tx.observer.api.block.subscriber.RollbackSubscriber
import com.wavesenterprise.we.tx.observer.core.spring.metrics.AddableLongMetricsContainer

class MetricRollbackSubscriber(
    private val rollbackCountMetricsContainer: AddableLongMetricsContainer,
) : RollbackSubscriber {
    override fun onRollback(weRollbackInfo: WeRollbackInfo) {
        rollbackCountMetricsContainer.add(1)
    }
}
