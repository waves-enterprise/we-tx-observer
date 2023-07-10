package com.wavesenterprise.we.tx.observer.starter

import com.wavesenterprise.we.tx.observer.core.spring.executor.AppContextPollingTxSubscriber
import com.wavesenterprise.we.tx.observer.core.spring.executor.ScheduledPartitionPoller
import com.wavesenterprise.we.tx.observer.core.spring.partition.DefaultLatestTxPartitionPoller
import com.wavesenterprise.we.tx.observer.core.spring.partition.ErrorHandlingLatestTxPartitionPoller
import com.wavesenterprise.we.tx.observer.core.spring.partition.LatestTxPartitionPoller
import com.wavesenterprise.we.tx.observer.core.spring.partition.PartitionHandler
import com.wavesenterprise.we.tx.observer.core.spring.partition.PollingTxSubscriber
import com.wavesenterprise.we.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.we.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.we.tx.observer.starter.properties.PartitionPollerProperties
import com.wavesenterprise.we.tx.observer.starter.properties.TxPollerProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    TxObserverJpaConfig::class,
    PartitionHandlerConfig::class,
)
@EnableConfigurationProperties(
    PartitionPollerProperties::class,
    TxPollerProperties::class,
)
class PartitionPollerConfig {
    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Bean
    fun scheduledPartitionPoller(
        errorHandlingLatestTxPartitionPoller: LatestTxPartitionPoller,
    ): ScheduledPartitionPoller = ScheduledPartitionPoller(
        errorHandlingLatestTxPartitionPoller = errorHandlingLatestTxPartitionPoller,
    )

    @Bean
    fun errorHandlingLatestTxPartitionPoller(
        defaultLatestTxPartitionPoller: LatestTxPartitionPoller,
        partitionHandler: PartitionHandler,
    ): LatestTxPartitionPoller = ErrorHandlingLatestTxPartitionPoller(
        defaultLatestTxPartitionPoller = defaultLatestTxPartitionPoller,
        partitionHandler = partitionHandler,
    )

    @Bean
    fun defaultLatestTxPartitionPoller(
        txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository,
        pollingTxSubscriber: PollingTxSubscriber,
        partitionHandler: PartitionHandler,
        partitionPollerProperties: PartitionPollerProperties,
    ): LatestTxPartitionPoller = DefaultLatestTxPartitionPoller(
        txQueuePartitionJpaRepository = txQueuePartitionJpaRepository,
        pollingTxSubscriber = pollingTxSubscriber,
        partitionHandler = partitionHandler,
    )

    @Bean
    fun applicationContextSourceExecutor(
        enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
        partitionHandler: PartitionHandler,
        txPollerProperties: TxPollerProperties,
    ): PollingTxSubscriber =
        AppContextPollingTxSubscriber(
            enqueuedTxJpaRepository = enqueuedTxJpaRepository,
            applicationContext = applicationContext,
            partitionHandler = partitionHandler,
            dequeSize = txPollerProperties.size,
        )
}
