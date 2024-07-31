package com.wavesenterprise.sdk.tx.observer.starter.properties

import com.wavesenterprise.sdk.tx.observer.core.spring.properties.TxObserverSchedulerConfig
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("tx-observer.scheduler")
data class TxObserverSchedulerProperties(
    override var poolSize: Int = 5,
) : TxObserverSchedulerConfig
