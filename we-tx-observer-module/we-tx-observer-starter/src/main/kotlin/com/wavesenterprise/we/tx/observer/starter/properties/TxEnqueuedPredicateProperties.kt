package com.wavesenterprise.we.tx.observer.starter.properties

import com.wavesenterprise.we.tx.observer.core.spring.properties.TxEnqueuedPredicateConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue
import java.time.Duration

@ConfigurationProperties("tx-observer.predicate")
@ConstructorBinding
data class TxEnqueuedPredicateProperties(
    override var txTypes: List<Int> = listOf(),
    @DefaultValue("3m")
    override val cacheTtl: Duration,
) : TxEnqueuedPredicateConfig
