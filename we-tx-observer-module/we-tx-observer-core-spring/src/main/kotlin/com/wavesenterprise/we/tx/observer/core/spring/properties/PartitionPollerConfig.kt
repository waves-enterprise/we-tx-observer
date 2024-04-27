package com.wavesenterprise.we.tx.observer.core.spring.properties

import java.time.Duration

interface PartitionPollerConfig {
    var enabled: Boolean
    var fixedDelay: Duration
    var threadCount: Int
    var accelerateAtQueueSize: Long
}
