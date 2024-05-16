package com.wavesenterprise.we.tx.observer.starter

import com.wavesenterprise.we.tx.observer.core.spring.executor.ScheduledPartitionCleaner
import com.wavesenterprise.we.tx.observer.core.spring.properties.PartitionCleanerConfig
import com.wavesenterprise.we.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.we.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.we.tx.observer.starter.properties.PartitionCleanerProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(TxObserverJpaConfig::class)
@EnableConfigurationProperties(PartitionCleanerProperties::class)
class PartitionCleanerConfig {

    @Bean
    fun scheduledPartitionCleaner(
        txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository,
        partitionCleanerConfig: PartitionCleanerConfig,
    ) =
        ScheduledPartitionCleaner(
            txQueuePartitionJpaRepository = txQueuePartitionJpaRepository,
            partitionCleanerConfig = partitionCleanerConfig,
        )
}
