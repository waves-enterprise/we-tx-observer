package com.wavesenterprise.we.tx.observer.starter.properties

import com.wavesenterprise.we.tx.observer.core.spring.properties.MetricsCollectorConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.boot.convert.DurationUnit
import java.time.Duration
import java.time.temporal.ChronoUnit

@ConfigurationProperties("tx-observer.metrics-collector")
@ConstructorBinding
data class MetricsCollectorProperties(
    @DefaultValue("true")
    override var enabled: Boolean,
    @DurationUnit(ChronoUnit.MILLIS) @DefaultValue("10s")
    override var fixedDelay: Duration,
) : MetricsCollectorConfig
