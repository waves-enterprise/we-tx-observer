package com.wavesenterprise.sdk.tx.observer.starter.observer.config

import com.wavesenterprise.sdk.tx.observer.starter.observer.web.service.TransactionalRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TxRunnerConfig {
    @Bean
    fun txRunner() = TransactionalRunner()
}
