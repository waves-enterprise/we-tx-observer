package com.wavesenterprise.sdk.tx.observer.starter.properties

import com.wavesenterprise.sdk.tx.observer.core.spring.properties.PartitionCleanerConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue
import java.time.Duration

@ConfigurationProperties("tx-observer.partition-cleaner")
@ConstructorBinding
data class PartitionCleanerProperties(
    @DefaultValue("true")
    override var enabled: Boolean,
    @DefaultValue("5m")
    override var fixedDelay: Duration,
    @DefaultValue("100")
    override var batchSize: Int,
) : PartitionCleanerConfig
