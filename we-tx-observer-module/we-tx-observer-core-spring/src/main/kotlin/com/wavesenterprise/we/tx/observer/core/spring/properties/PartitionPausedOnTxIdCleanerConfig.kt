package com.wavesenterprise.we.tx.observer.core.spring.properties

import java.time.Duration

interface PartitionPausedOnTxIdCleanerConfig {
    var enabled: Boolean
    var fixedDelay: Duration
}
