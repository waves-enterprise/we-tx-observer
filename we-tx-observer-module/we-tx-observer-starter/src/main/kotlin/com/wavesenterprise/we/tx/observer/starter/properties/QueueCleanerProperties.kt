package com.wavesenterprise.we.tx.observer.starter.properties

import com.wavesenterprise.we.tx.observer.core.spring.properties.QueueCleanerConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue

@ConfigurationProperties("tx-observer.queue-cleaner")
@ConstructorBinding
data class QueueCleanerProperties(
    @DefaultValue("true")
    override var enabled: Boolean,
    override var archiveHeightWindow: Long = 100,
    override var deleteBatchSize: Long = 100,
    override var cleanCronExpression: String = "0 0/5 * * * ?",
    override var lockAtLeast: Long = 0,
    override var lockAtMost: Long = 10000,
) : QueueCleanerConfig
