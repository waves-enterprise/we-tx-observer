package com.wavesenterprise.we.tx.tracker.starter.properties

import com.wavesenterprise.we.tx.tracker.core.spring.properties.SuccessSubscriberConfig
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("tx-tracker.success-subscriber")
class SuccessSubscriberProperties(
    override var enabled: Boolean = true,
) : SuccessSubscriberConfig
