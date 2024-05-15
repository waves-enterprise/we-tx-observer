package com.wavesenterprise.we.tx.observer.core.spring.properties

import java.time.Duration

interface PartitionCleanerConfig {
    var enabled: Boolean
    var fixedDelay: Duration
}
