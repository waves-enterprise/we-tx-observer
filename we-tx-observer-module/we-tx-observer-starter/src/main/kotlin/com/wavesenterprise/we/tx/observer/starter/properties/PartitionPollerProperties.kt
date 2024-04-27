package com.wavesenterprise.we.tx.observer.starter.properties

import com.wavesenterprise.we.tx.observer.core.spring.properties.PartitionPollerConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.boot.convert.DurationUnit
import java.time.Duration
import java.time.temporal.ChronoUnit

@ConfigurationProperties("tx-observer.partition-poller")
@ConstructorBinding
data class PartitionPollerProperties(
    @DefaultValue("true")
    override var enabled: Boolean,
    @DurationUnit(ChronoUnit.MILLIS) @DefaultValue("500ms")
    override var fixedDelay: Duration,
    @DefaultValue("4")
    override var threadCount: Int,
    @DefaultValue("200")
    override var accelerateAtQueueSize: Long,
) : PartitionPollerConfig
