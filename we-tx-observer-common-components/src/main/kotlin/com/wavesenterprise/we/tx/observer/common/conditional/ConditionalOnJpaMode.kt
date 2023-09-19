package com.wavesenterprise.we.tx.observer.common.conditional

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS
)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(
    name = ["tx-observer.queue-mode"],
    havingValue = "JPA",
    matchIfMissing = true,
)
annotation class ConditionalOnJpaMode
