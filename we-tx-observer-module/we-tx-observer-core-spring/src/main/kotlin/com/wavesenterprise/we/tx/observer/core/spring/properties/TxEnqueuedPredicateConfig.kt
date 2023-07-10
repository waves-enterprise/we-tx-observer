package com.wavesenterprise.we.tx.observer.core.spring.properties

import java.time.Duration

interface TxEnqueuedPredicateConfig {
    var txTypes: List<Int>
    val cacheTtl: Duration
}
