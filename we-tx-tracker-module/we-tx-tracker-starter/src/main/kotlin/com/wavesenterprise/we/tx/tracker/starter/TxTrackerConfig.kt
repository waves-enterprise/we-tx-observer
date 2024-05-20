package com.wavesenterprise.we.tx.tracker.starter

import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.node.client.blocking.tx.TxService
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.spring.autoconfigure.node.NodeBlockingServiceFactoryAutoConfiguration
import com.wavesenterprise.we.tx.observer.common.conditional.ConditionalOnTracker
import com.wavesenterprise.we.tx.observer.common.tx.handler.TxHandler
import com.wavesenterprise.we.tx.observer.common.tx.subscriber.TxSubscriberImpl
import com.wavesenterprise.we.tx.tracker.api.TxTracker
import com.wavesenterprise.we.tx.tracker.api.TxTrackerActualTxResolver
import com.wavesenterprise.we.tx.tracker.core.spring.component.JpaTxTracker
import com.wavesenterprise.we.tx.tracker.core.spring.component.ScheduledTxTracker
import com.wavesenterprise.we.tx.tracker.core.spring.component.TxTrackerActualTxResolverImpl
import com.wavesenterprise.we.tx.tracker.core.spring.component.TxTrackerPredicate
import com.wavesenterprise.we.tx.tracker.core.spring.component.TxTrackerTxHandler
import com.wavesenterprise.we.tx.tracker.jpa.config.TxTrackerJpaConfig
import com.wavesenterprise.we.tx.tracker.jpa.repository.BusinessObjectInfoJpaRepository
import com.wavesenterprise.we.tx.tracker.jpa.repository.SmartContractInfoJpaRepository
import com.wavesenterprise.we.tx.tracker.jpa.repository.TxTrackerJpaRepository
import com.wavesenterprise.we.tx.tracker.starter.properties.SuccessSubscriberProperties
import com.wavesenterprise.we.tx.tracker.starter.properties.TxTrackerProperties
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    TxTrackerJpaConfig::class,
    TxTrackerSchedulerConfig::class,
)
@EnableConfigurationProperties(TxTrackerProperties::class, SuccessSubscriberProperties::class)
@ConditionalOnTracker
@AutoConfigureAfter(NodeBlockingServiceFactoryAutoConfiguration::class)
class TxTrackerConfig {

    @Bean
    @ConditionalOnMissingBean
    fun jpaTxTracker(
        txTrackerJpaRepository: TxTrackerJpaRepository,
        smartContractInfoJpaRepository: SmartContractInfoJpaRepository,
        businessObjectInfoJpaRepository: BusinessObjectInfoJpaRepository,
        txService: TxService,
        txTrackerProperties: TxTrackerProperties,
    ): TxTracker = JpaTxTracker(
        txTrackerJpaRepository = txTrackerJpaRepository,
        smartContractInfoJpaRepository = smartContractInfoJpaRepository,
        businessObjectInfoJpaRepository = businessObjectInfoJpaRepository,
        txService = txService,
        txTrackerProperties = txTrackerProperties,
    )

    @Bean
    fun txTrackingPredicate(
        txTrackerActualTxResolver: TxTrackerActualTxResolver,
        txTracker: TxTracker,
    ) = TxTrackerPredicate(
        txTrackerActualTxResolver = txTrackerActualTxResolver,
        txTracker = txTracker,
    )

    @Bean
    fun txTrackingHandler(
        txTrackerActualTxResolver: TxTrackerActualTxResolver,
        txTracker: TxTracker,
    ): TxHandler =
        TxTrackerTxHandler(
            txTrackerActualTxResolver = txTrackerActualTxResolver,
            txTracker = txTracker,
        )

    @Bean
    fun txTrackerActualTxResolver(): TxTrackerActualTxResolver = TxTrackerActualTxResolverImpl

    @Bean
    @ConditionalOnProperty(
        name = ["tx-tracker.success-subscriber.enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun txTrackingSubscriber(
        txTrackingPredicate: (Tx) -> Boolean,
        txTrackingTxHandler: TxHandler,
    ) = TxSubscriberImpl(
        predicate = txTrackingPredicate,
        txHandlers = listOf(txTrackingTxHandler),
    )

    @Bean
    fun scheduledTxTracker(
        nodeBlockingServiceFactory: NodeBlockingServiceFactory,
        txTracker: TxTracker,
        txTrackerProperties: TxTrackerProperties,
    ) = ScheduledTxTracker(
        nodeBlockingServiceFactory = nodeBlockingServiceFactory,
        txTracker = txTracker,
        txTrackerProperties = txTrackerProperties,
    )
}
