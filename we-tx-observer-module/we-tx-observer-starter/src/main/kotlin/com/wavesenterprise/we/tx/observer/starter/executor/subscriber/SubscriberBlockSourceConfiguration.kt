package com.wavesenterprise.we.tx.observer.starter.executor.subscriber

import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.sdk.node.client.grpc.blocking.GrpcNodeClientParams
import com.wavesenterprise.sdk.node.client.grpc.blocking.factory.GrpcNodeServiceFactoryFactory
import com.wavesenterprise.sdk.spring.autoconfigure.node.NodeBlockingServiceFactoryAutoConfiguration
import com.wavesenterprise.sdk.spring.autoconfigure.node.properties.NodeProperties
import com.wavesenterprise.we.tx.observer.api.block.subscriber.BlockSubscriber
import com.wavesenterprise.we.tx.observer.api.block.subscriber.RollbackSubscriber
import com.wavesenterprise.we.tx.observer.common.tx.executor.TxExecutor
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.EventSubscriber
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.HandleRollbackFactory
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.HandleRollbackFactoryImpl
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.strategy.AppendedBlockHistoryBuffer
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.strategy.AppendedBlockHistoryBufferImpl
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.strategy.BlockAppendedEventHandlingStrategyFactory
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.strategy.EventHandlingStrategyFactory
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.strategy.MicroBlockEventHandlingStrategyFactory
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.we.tx.observer.core.spring.lock.LockService
import com.wavesenterprise.we.tx.observer.core.spring.properties.Strategy
import com.wavesenterprise.we.tx.observer.starter.executor.BLOCK_SOURCE_MODE
import com.wavesenterprise.we.tx.observer.starter.properties.SubscriberProperties
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(
    prefix = "tx-observer",
    name = [BLOCK_SOURCE_MODE],
    havingValue = "subscriber"
)
@EnableConfigurationProperties(
    SubscriberProperties::class
)
@AutoConfigureAfter(NodeBlockingServiceFactoryAutoConfiguration::class)
class SubscriberBlockSourceConfiguration(
    private val subscriberProperties: SubscriberProperties,
    private val nodeProperties: NodeProperties,
    private val blocksService: BlocksService,
    private val syncInfoService: SyncInfoService,
    private val txExecutor: TxExecutor,
    private val lockService: LockService,
    private val blockSubscribers: List<BlockSubscriber>,
    private val rollbackSubscribers: List<RollbackSubscriber>,
) {
    @Bean
    fun eventSubscriber(
        eventHandlingStrategyFactory: EventHandlingStrategyFactory
    ): EventSubscriber =
        EventSubscriber(
            syncInfoService = syncInfoService,
            weBlockchainEventServices = nodeProperties.config.entries.map { (key, node) ->
                val grpc = requireNotNull(node.grpc) {
                    "GRPC properties should be specified for '$key' node when using tx-observer in SUBSCRIBER mode"
                }
                GrpcNodeServiceFactoryFactory.createClient(
                    grpcProperties = GrpcNodeClientParams(
                        address = grpc.address,
                        port = grpc.port,
                        keepAliveTime = grpc.keepAliveTime,
                        keepAliveWithoutCalls = grpc.keepAliveWithoutCalls,
                    )
                ).blockchainEventsService()
            },
            eventHandlingStrategyFactory = eventHandlingStrategyFactory,
            blockSubscribers = blockSubscribers,
            rollbackSubscribers = rollbackSubscribers,
            txExecutor = txExecutor,
            lockService = lockService,
        )

    @Bean
    fun eventHandlingStrategyFactory(
        handleRollbackFactory: HandleRollbackFactory,
        appendedBlockHistoryBuffer: AppendedBlockHistoryBuffer,
    ): EventHandlingStrategyFactory =
        when (subscriberProperties.strategy) {
            Strategy.MICRO_BLOCK -> MicroBlockEventHandlingStrategyFactory(
                handleRollbackFactory = handleRollbackFactory,
                appendedBlockHistoryBuffer = appendedBlockHistoryBuffer,
            )
            Strategy.BLOCK_APPENDED -> BlockAppendedEventHandlingStrategyFactory(
                handleRollbackFactory = handleRollbackFactory,
                appendedBlockHistoryBuffer = appendedBlockHistoryBuffer,
            )
        }

    @Bean
    fun manageRollbackFactory(): HandleRollbackFactory =
        HandleRollbackFactoryImpl(
            blocksService = blocksService,
        )

    @Bean
    fun appendedBlockHistoryBuffer(): AppendedBlockHistoryBuffer =
        with(subscriberProperties.blockBuffer) {
            AppendedBlockHistoryBufferImpl(
                maxCount = maxCount,
                maxSizeBytes = maxSize.toBytes(),
            )
        }
}
