package com.wavesenterprise.we.tx.observer.starter.properties

import com.wavesenterprise.we.tx.observer.core.spring.properties.PrivacyAvailabilityCheckConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.boot.convert.DurationUnit
import java.time.Duration
import java.time.temporal.ChronoUnit

@ConfigurationProperties("tx-observer.privacy-check")
@ConstructorBinding
data class PrivacyAvailabilityCheckProperties(

    @DefaultValue("true")
    override var enabled: Boolean,

    @DurationUnit(ChronoUnit.MILLIS)
    @DefaultValue("500ms")
    override val fixedDelay: Duration,

    @DefaultValue("3")
    override val threadCount: Int,

    override val limitForOld: Int = 25,
    override val limitForRecent: Int = 10,
) : PrivacyAvailabilityCheckConfig
