package com.wavesenterprise.sdk.tx.observer.starter.executor.syncinfo

import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.NODE_HEIGHT
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.OBSERVER_HEIGHT
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo.BlockHistoryCleaner
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo.BlockHistoryService
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo.BlockHistoryServiceImpl
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo.SyncInfoServiceImpl
import com.wavesenterprise.sdk.tx.observer.core.spring.metrics.MetricContainerData
import com.wavesenterprise.sdk.tx.observer.core.spring.metrics.MetricsContainer
import com.wavesenterprise.sdk.tx.observer.jpa.repository.BlockHeightJpaRepository
import com.wavesenterprise.sdk.tx.observer.jpa.repository.BlockHeightResetRepository
import com.wavesenterprise.sdk.tx.observer.jpa.repository.BlockHistoryRepository
import com.wavesenterprise.sdk.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.sdk.tx.observer.starter.properties.TxObserverProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
    TxObserverProperties::class
)
class SyncInfoConfig(
    private val blocksService: BlocksService,
    private val txObserverProperties: TxObserverProperties,
    private val blockHeightJpaRepository: BlockHeightJpaRepository,
    private val blockHistoryRepository: BlockHistoryRepository,
    private val blockHeightResetRepository: BlockHeightResetRepository,
    private val enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
) {

    @Bean
    fun blockInfoService(
        blockHistoryService: BlockHistoryService,
        nodeHeightMetric: MetricsContainer,
        observerHeightMetric: MetricsContainer,
    ): SyncInfoService =
        SyncInfoServiceImpl(
            blockHeightJpaRepository = blockHeightJpaRepository,
            blockHeightResetRepository = blockHeightResetRepository,
            enqueuedTxJpaRepository = enqueuedTxJpaRepository,
            blockHistoryService = blockHistoryService,
            blocksService = blocksService,
            syncHistory = SyncInfoServiceImpl.SyncHistoryProperties(
                enabled = txObserverProperties.syncHistory,
                fromHeight = txObserverProperties.activationHeight
            ),
            autoResetHeight = txObserverProperties.autoResetHeight,
            forkNotResolvedHeightDrop = txObserverProperties.forkNotResolvedHeightDrop,
            nodeHeightMetric = nodeHeightMetric,
            observerHeightMetric = observerHeightMetric
        )

    @Bean
    fun nodeHeightMetric() = MetricContainerData(
        metricName = NODE_HEIGHT,
    )

    @Bean
    fun observerHeightMetric() = MetricContainerData(
        metricName = OBSERVER_HEIGHT,
    )

    @Bean
    fun blockHistoryService(): BlockHistoryService =
        BlockHistoryServiceImpl(
            blockHistoryRepository = blockHistoryRepository,
            historyDepth = txObserverProperties.blockHistoryDepth,
            blocksService = blocksService,
            blockWindowSize = txObserverProperties.blockHeightWindow,
        )

    @Bean
    fun blockHistoryCleaner(): BlockHistoryCleaner =
        BlockHistoryCleaner(
            blockHistoryRepository = blockHistoryRepository,
        )
}
