package com.wavesenterprise.sdk.tx.observer.core.spring.properties

interface QueueCleanerConfig {
    var enabled: Boolean
    var archiveHeightWindow: Long
    var deleteBatchSize: Long
    var cleanCronExpression: String
}
