package com.wavesenterprise.we.tx.observer.starter.properties

import com.wavesenterprise.we.tx.observer.core.spring.properties.PartitionPausedOnTxIdCleanerConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.boot.convert.DurationUnit
import java.time.Duration
import java.time.temporal.ChronoUnit

@ConfigurationProperties("tx-observer.partition-paused-on-tx-id-cleaner")
@ConstructorBinding
data class PartitionPausedOnTxIdCleanerProperties(
    @DefaultValue("true")
    override var enabled: Boolean,
    @DurationUnit(ChronoUnit.MILLIS) @DefaultValue("5m")
    override var fixedDelay: Duration
) : PartitionPausedOnTxIdCleanerConfig
