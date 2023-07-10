package com.wavesenterprise.we.tx.observer.core.spring.properties

import java.time.Duration

interface PrivacyAvailabilityCheckConfig {
    val enabled: Boolean
    val fixedDelay: Duration
    val threadCount: Int
    val limitForOld: Int
    val limitForRecent: Int
}
