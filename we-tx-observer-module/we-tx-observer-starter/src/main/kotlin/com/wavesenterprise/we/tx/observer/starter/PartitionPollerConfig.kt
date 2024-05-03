package com.wavesenterprise.we.tx.observer.starter

import com.wavesenterprise.we.tx.observer.core.spring.executor.AppContextPollingTxSubscriber
import com.wavesenterprise.we.tx.observer.core.spring.executor.ScheduledPartitionPoller
import com.wavesenterprise.we.tx.observer.core.spring.partition.DefaultTxPartitionPoller
import com.wavesenterprise.we.tx.observer.core.spring.partition.DefaultTxPartitionPollerAccelerationHelper
import com.wavesenterprise.we.tx.observer.core.spring.partition.ErrorHandlingTxPartitionPoller
import com.wavesenterprise.we.tx.observer.core.spring.partition.PartitionHandler
import com.wavesenterprise.we.tx.observer.core.spring.partition.PollingTxSubscriber
import com.wavesenterprise.we.tx.observer.core.spring.partition.TxPartitionPoller
import com.wavesenterprise.we.tx.observer.core.spring.partition.TxPartitionPollerAccelerationHelper
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
        errorHandlingTxPartitionPoller: TxPartitionPoller,
    ): ScheduledPartitionPoller = ScheduledPartitionPoller(
        errorHandlingTxPartitionPoller = errorHandlingTxPartitionPoller,
    )

    @Bean
    fun errorHandlingTxPartitionPoller(
        defaultTxPartitionPoller: TxPartitionPoller,
        partitionHandler: PartitionHandler,
    ): TxPartitionPoller = ErrorHandlingTxPartitionPoller(
        defaultTxPartitionPoller = defaultTxPartitionPoller,
        partitionHandler = partitionHandler,
    )

    @Bean
    fun txPartitionPollerAccelerationHelper(
        enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
        partitionPollerProperties: PartitionPollerProperties,
    ): TxPartitionPollerAccelerationHelper = DefaultTxPartitionPollerAccelerationHelper(
        enqueuedTxJpaRepository = enqueuedTxJpaRepository,
        partitionPollerProperties = partitionPollerProperties,
    )

    @Bean
    fun defaultTxPartitionPoller(
        txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository,
        enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
        pollingTxSubscriber: PollingTxSubscriber,
        partitionHandler: PartitionHandler,
        txPartitionPollerAccelerationHelper: TxPartitionPollerAccelerationHelper,
    ): TxPartitionPoller = DefaultTxPartitionPoller(
        txQueuePartitionJpaRepository = txQueuePartitionJpaRepository,
        enqueuedTxJpaRepository = enqueuedTxJpaRepository,
        pollingTxSubscriber = pollingTxSubscriber,
        partitionHandler = partitionHandler,
        txPartitionPollerAccelerationHelper = txPartitionPollerAccelerationHelper,
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
