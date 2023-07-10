package com.wavesenterprise.we.tx.observer.starter.metrics

import com.wavesenterprise.we.tx.observer.core.spring.metrics.MetricsContainer
import com.wavesenterprise.we.tx.observer.core.spring.metrics.MetricsSetter
import com.wavesenterprise.we.tx.observer.starter.TxObserverStarterConfig
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnClass(MeterRegistry::class)
@ConditionalOnBean(MeterRegistry::class)
@AutoConfigureAfter(
    TxObserverStarterConfig::class,
    MetricsAutoConfiguration::class,
    SimpleMetricsExportAutoConfiguration::class,
)
class TxObserverMetricsAutoConfig {

    @Bean
    fun metricsSetter(
        meterRegistry: MeterRegistry,
        metrics: MutableList<MetricsContainer>,
    ) = MetricsSetter(
        meterRegistry = meterRegistry,
        metrics = metrics,
    )
}
