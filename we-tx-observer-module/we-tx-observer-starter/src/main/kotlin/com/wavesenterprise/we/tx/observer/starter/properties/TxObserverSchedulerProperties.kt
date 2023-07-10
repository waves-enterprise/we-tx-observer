package com.wavesenterprise.we.tx.observer.starter.properties

import com.wavesenterprise.we.tx.observer.core.spring.properties.TxObserverSchedulerConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("tx-observer.scheduler")
@ConstructorBinding
data class TxObserverSchedulerProperties(
    override var poolSize: Int = 5,
) : TxObserverSchedulerConfig
