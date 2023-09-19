package com.wavesenterprise.we.tx.observer.starter.observer.executor

import com.wavesenterprise.we.tx.observer.core.spring.executor.MetricRollbackSubscriber
import com.wavesenterprise.we.tx.observer.core.spring.metrics.AddableLongMetricsContainer
import io.mockk.confirmVerified
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class MetricRollbackTxSubscriberTest {
    @RelaxedMockK
    private lateinit var rollbackCountMetricsContainer: AddableLongMetricsContainer

    @InjectMockKs
    private lateinit var metricRollbackSubscriber: MetricRollbackSubscriber

    @Test
    fun `should increment metric`() {
        metricRollbackSubscriber.onRollback(mockk())

        verify {
            rollbackCountMetricsContainer.add(1)
        }
        confirmVerified(rollbackCountMetricsContainer)
    }
}
