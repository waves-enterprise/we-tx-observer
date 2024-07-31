package com.wavesenterprise.sdk.tx.observer.starter.properties

import com.wavesenterprise.sdk.tx.observer.core.spring.properties.TxEnqueuedPredicateConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import java.time.Duration

@ConfigurationProperties("tx-observer.predicate")
data class TxEnqueuedPredicateProperties(
    override var txTypes: List<Int> = listOf(),
    @DefaultValue("3m")
    override val cacheTtl: Duration,
) : TxEnqueuedPredicateConfig
