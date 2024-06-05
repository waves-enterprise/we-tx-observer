package com.wavesenterprise.sdk.tx.observer.starter

import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.ScheduledForkResolver
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.sdk.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.sdk.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.sdk.tx.observer.starter.properties.ForkResolverProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(TxObserverJpaConfig::class)
@EnableConfigurationProperties(ForkResolverProperties::class)
class ForkResolverConfig {
    @Autowired
    lateinit var nodeBlockingServiceFactory: NodeBlockingServiceFactory

    @Bean
    fun scheduledForkResolver(
        enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
        syncInfoService: SyncInfoService,
        forkResolverProperties: ForkResolverProperties,
    ) = ScheduledForkResolver(
        enqueuedTxJpaRepository = enqueuedTxJpaRepository,
        syncInfoService = syncInfoService,
        nodeBlockingServiceFactory = nodeBlockingServiceFactory,
        forkHeightOffset = forkResolverProperties.heightOffset,
        forkCheckSize = forkResolverProperties.window,
    )
}
