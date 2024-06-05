package com.wavesenterprise.sdk.tx.observer.core.spring.properties

import java.time.Duration

interface ForkResolverConfig {
    var enabled: Boolean
    var fixedDelay: Duration
    var heightOffset: Long
    var window: Int
}
