package com.wavesenterprise.sdk.tx.observer.jpa.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import java.time.OffsetDateTime
import java.util.Optional

@Configuration
class DateTimeProviderConfig {

    @Bean
    @ConditionalOnMissingBean
    fun dateTimeProvider() = DateTimeProvider {
        Optional.of(OffsetDateTime.now())
    }
}
