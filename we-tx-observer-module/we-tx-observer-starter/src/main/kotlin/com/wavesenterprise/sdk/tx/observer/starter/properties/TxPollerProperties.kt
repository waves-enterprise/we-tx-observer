package com.wavesenterprise.sdk.tx.observer.starter.properties

import com.wavesenterprise.sdk.tx.observer.core.spring.properties.TxPollerConfig
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("tx-observer.tx-poller")
data class TxPollerProperties(
    override var size: Int = 100,
) : TxPollerConfig
