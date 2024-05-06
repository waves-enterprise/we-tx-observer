package com.wavesenterprise.we.tx.observer.starter.executor.poller

import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.we.tx.observer.api.block.subscriber.BlockSubscriber
import com.wavesenterprise.we.tx.observer.common.tx.executor.TxExecutor
import com.wavesenterprise.we.tx.observer.core.spring.executor.poller.BlocksLoader
import com.wavesenterprise.we.tx.observer.core.spring.executor.poller.BlocksLoaderImpl
import com.wavesenterprise.we.tx.observer.core.spring.executor.poller.ScheduledBlockInfoSynchronizer
import com.wavesenterprise.we.tx.observer.core.spring.executor.poller.SourceExecutor
import com.wavesenterprise.we.tx.observer.core.spring.executor.poller.SourceExecutorImpl
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.we.tx.observer.starter.executor.BLOCK_SOURCE_MODE
import com.wavesenterprise.we.tx.observer.starter.properties.TxObserverProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(
    prefix = "tx-observer",
    name = [BLOCK_SOURCE_MODE],
    havingValue = "poller",
    matchIfMissing = true,
)
@EnableConfigurationProperties(
    TxObserverProperties::class,
)
class PollerBlockSourceConfiguration(
    private val blocksService: BlocksService,
    private val txObserverProperties: TxObserverProperties,
    private val syncInfoService: SyncInfoService,
    private val blockSubscribers: List<BlockSubscriber>,
) {
    @Bean
    fun scheduledSourceExecutor(
        sourceExecutor: SourceExecutor,
        txExecutor: TxExecutor,
        enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
    ) =
        ScheduledBlockInfoSynchronizer(
            sourceExecutor = sourceExecutor,
            syncInfoService = syncInfoService,
            enqueuedTxJpaRepository = enqueuedTxJpaRepository,
            pauseSyncAtQueueSize = txObserverProperties.pauseSyncAtQueueSize,
            blockHeightWindow = txObserverProperties.blockHeightWindow,
            liquidBlockPollingDelay = txObserverProperties.liquidBlockPollingDelay,
            txExecutor = txExecutor,
        )

    @Bean
    fun sourceExecutor(
        blocksLoader: BlocksLoader,
    ): SourceExecutor =
        SourceExecutorImpl(
            blockSubscribers = blockSubscribers,
            blocksLoader = blocksLoader,
        )

    @Bean
    fun blocksLoader(): BlocksLoader =
        BlocksLoaderImpl(
            blocksService = blocksService,
            downloadWindowSize = txObserverProperties.blockSizeWindow.toBytes(),
        )
}
