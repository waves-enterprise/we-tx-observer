package com.wavesenterprise.we.tx.observer.starter

import com.wavesenterprise.we.tx.observer.core.spring.executor.ScheduledPartitionPausedOnTxIdCleaner
import com.wavesenterprise.we.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.we.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.we.tx.observer.starter.properties.PartitionPausedOnTxIdCleanerProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(TxObserverJpaConfig::class)
@EnableConfigurationProperties(
    PartitionPausedOnTxIdCleanerProperties::class,
)
class PartitionPausedOnTxIdCleanerConfig {

    @Bean
    fun scheduledPartitionPausedOnTxIdCleaner(
        txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository,
    ) =
        ScheduledPartitionPausedOnTxIdCleaner(
            txQueuePartitionJpaRepository = txQueuePartitionJpaRepository,
        )
}
