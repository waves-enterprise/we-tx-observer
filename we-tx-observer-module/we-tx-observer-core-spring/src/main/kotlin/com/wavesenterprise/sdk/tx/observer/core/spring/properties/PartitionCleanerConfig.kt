package com.wavesenterprise.sdk.tx.observer.core.spring.properties

import java.time.Duration

interface PartitionCleanerConfig {
    var enabled: Boolean
    var fixedDelay: Duration
    var batchSize: Int
}
