package com.wavesenterprise.sdk.tx.observer.starter.properties

import com.wavesenterprise.sdk.tx.observer.core.spring.properties.PartitionPollerConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import java.time.Duration

@ConfigurationProperties("tx-observer.partition-poller")
data class PartitionPollerProperties(
    @DefaultValue("true")
    override var enabled: Boolean,
    @DefaultValue("50ms")
    override var fixedDelay: Duration,
    @DefaultValue("4")
    override var threadCount: Int,
    @DefaultValue("200")
    override var accelerateAtQueueSize: Long,
) : PartitionPollerConfig
