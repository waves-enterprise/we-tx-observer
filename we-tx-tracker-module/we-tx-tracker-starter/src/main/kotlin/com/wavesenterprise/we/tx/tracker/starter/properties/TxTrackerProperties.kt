package com.wavesenterprise.we.tx.tracker.starter.properties

import com.wavesenterprise.we.tx.tracker.core.spring.properties.TxTrackerProps
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.boot.convert.DurationUnit
import java.time.Duration
import java.time.temporal.ChronoUnit

@ConfigurationProperties("tx-tracker")
@ConstructorBinding
data class TxTrackerProperties(
    @DefaultValue("false")
    override var enabled: Boolean,
    override var findContractInNode: Boolean = true,
    @DurationUnit(ChronoUnit.MILLIS) @DefaultValue("10s")
    override var fixedDelay: Duration,
    override var lockAtLeast: Long = 0,
    override var lockAtMost: Long = 10000,
    override var trackedTxPageRequestLimit: Int = 100,
    @DurationUnit(ChronoUnit.SECONDS) @DefaultValue("2H")
    override var timeout: Duration,
    override var minContractTxErrorCount: Int = 1,
    override var minContractTxFailureCount: Int = 1,
    override var failOnRecoverableContractTxError: Boolean = true,
) : TxTrackerProps
