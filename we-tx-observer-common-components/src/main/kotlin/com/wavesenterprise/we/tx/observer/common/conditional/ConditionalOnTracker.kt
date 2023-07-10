package com.wavesenterprise.we.tx.observer.common.conditional

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS
)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(
    name = ["tx-tracker.enabled"],
    havingValue = "true",
)
annotation class ConditionalOnTracker
