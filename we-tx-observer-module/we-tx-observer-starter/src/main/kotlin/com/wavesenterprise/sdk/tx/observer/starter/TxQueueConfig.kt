package com.wavesenterprise.sdk.tx.observer.starter

import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.tx.observer.api.block.subscriber.BlockSubscriber
import com.wavesenterprise.sdk.tx.observer.api.partition.TxQueuePartitionResolver
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.ScheduledTxQueueCleaner
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.sdk.tx.observer.core.spring.partition.DefaultPartitionResolver
import com.wavesenterprise.sdk.tx.observer.core.spring.partition.TxQueuePartitionResolveService
import com.wavesenterprise.sdk.tx.observer.core.spring.partition.TxQueuePartitionResolveServiceImpl
import com.wavesenterprise.sdk.tx.observer.core.spring.web.EnqueuedTxController
import com.wavesenterprise.sdk.tx.observer.core.spring.web.ObserverController
import com.wavesenterprise.sdk.tx.observer.core.spring.web.TxQueuePartitionController
import com.wavesenterprise.sdk.tx.observer.core.spring.web.WebControllers
import com.wavesenterprise.sdk.tx.observer.core.spring.web.service.TxQueueService
import com.wavesenterprise.sdk.tx.observer.core.spring.web.service.TxQueueStatusServiceImpl
import com.wavesenterprise.sdk.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.sdk.tx.observer.jpa.repository.BlockHeightResetRepository
import com.wavesenterprise.sdk.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.sdk.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.sdk.tx.observer.starter.properties.QueueCleanerProperties
import com.wavesenterprise.sdk.tx.observer.starter.properties.TxObserverProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ComponentScan.Filter
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import

@Configuration
@Import(TxObserverJpaConfig::class)
@ComponentScan(
    useDefaultFilters = false,
    basePackageClasses = [WebControllers::class],
    includeFilters = [
        Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [
                EnqueuedTxController::class,
                ObserverController::class,
                TxQueuePartitionController::class,
            ],
        ),
    ],
)
@EnableConfigurationProperties(
    TxObserverProperties::class,
    QueueCleanerProperties::class,
)
class TxQueueConfig {
    @Autowired
    lateinit var nodeBlockingServiceFactory: NodeBlockingServiceFactory

    @Suppress("LongParameterList")
    @Bean
    fun txQueueService(
        syncInfoService: SyncInfoService,
        enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
        blockHeightResetRepository: BlockHeightResetRepository,
        @Qualifier("enqueueingBlockSubscriber")
        enqueueingBlockSubscriber: BlockSubscriber,
        txObserverProperties: TxObserverProperties,
    ): TxQueueService = TxQueueStatusServiceImpl(
        nodeBlockingServiceFactory = nodeBlockingServiceFactory,
        syncInfoService = syncInfoService,
        enqueuedTxJpaRepository = enqueuedTxJpaRepository,
        blockHeightResetRepository = blockHeightResetRepository,
        enqueueingBlockSubscriber = enqueueingBlockSubscriber,
        errorPriorityOffset = txObserverProperties.errorPriorityOffset,
    )

    @Bean
    fun txQueuePartitionResolveService(
        txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository,
        partitionResolver: TxQueuePartitionResolver,
        txObserverProperties: TxObserverProperties,
    ): TxQueuePartitionResolveService =
        TxQueuePartitionResolveServiceImpl(
            txQueuePartitionJpaRepository = txQueuePartitionJpaRepository,
            partitionResolver = partitionResolver,
            defaultPartitionId = txObserverProperties.defaultPartitionId,
        )

    @Bean
    fun scheduledTxQueueCleaner(
        enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
        syncInfoService: SyncInfoService,
        queueCleanerProperties: QueueCleanerProperties,
    ) =
        ScheduledTxQueueCleaner(
            enqueuedTxJpaRepository = enqueuedTxJpaRepository,
            syncInfoService = syncInfoService,
            queueCleanerConfig = queueCleanerProperties,
        )

    @Bean
    @ConditionalOnMissingBean
    fun defaultPartitionResolver(): TxQueuePartitionResolver = DefaultPartitionResolver()
}
