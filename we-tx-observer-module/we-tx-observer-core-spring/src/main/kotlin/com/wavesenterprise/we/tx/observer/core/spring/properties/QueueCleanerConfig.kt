package com.wavesenterprise.we.tx.observer.core.spring.properties

interface QueueCleanerConfig {
    var enabled: Boolean
    var archiveHeightWindow: Long
    var deleteBatchSize: Long
    var cleanCronExpression: String
    var lockAtLeast: Long
    var lockAtMost: Long
}
