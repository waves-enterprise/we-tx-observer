package com.wavesenterprise.sdk.tx.observer.starter.properties

import com.wavesenterprise.sdk.tx.observer.core.spring.properties.ForkResolverConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.boot.convert.DurationUnit
import java.time.Duration
import java.time.temporal.ChronoUnit

@ConfigurationProperties("tx-observer.fork-resolver")
@ConstructorBinding
data class ForkResolverProperties(
    @DefaultValue("true")
    override var enabled: Boolean,
    @DurationUnit(ChronoUnit.MILLIS) @DefaultValue("5m")
    override var fixedDelay: Duration,
    override var heightOffset: Long = 10000,
    override var window: Int = 5,
) : ForkResolverConfig
