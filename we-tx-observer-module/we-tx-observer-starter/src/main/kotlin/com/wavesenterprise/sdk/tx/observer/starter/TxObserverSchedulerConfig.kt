package com.wavesenterprise.sdk.tx.observer.starter

import com.wavesenterprise.sdk.tx.observer.core.spring.executor.ScheduledForkResolver
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.ScheduledPartitionCleaner
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.ScheduledPartitionPausedOnTxIdCleaner
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.ScheduledPartitionPoller
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.ScheduledPrivacyChecker
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.ScheduledTxQueueCleaner
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.poller.ScheduledBlockInfoSynchronizer
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber.EventSubscriber
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo.BlockHistoryCleaner
import com.wavesenterprise.sdk.tx.observer.core.spring.metrics.ScheduledMetricsCollector
import com.wavesenterprise.sdk.tx.observer.starter.properties.ForkResolverProperties
import com.wavesenterprise.sdk.tx.observer.starter.properties.MetricsCollectorProperties
import com.wavesenterprise.sdk.tx.observer.starter.properties.PartitionCleanerProperties
import com.wavesenterprise.sdk.tx.observer.starter.properties.PartitionPausedOnTxIdCleanerProperties
import com.wavesenterprise.sdk.tx.observer.starter.properties.PartitionPollerProperties
import com.wavesenterprise.sdk.tx.observer.starter.properties.PrivacyAvailabilityCheckProperties
import com.wavesenterprise.sdk.tx.observer.starter.properties.QueueCleanerProperties
import com.wavesenterprise.sdk.tx.observer.starter.properties.TxObserverProperties
import com.wavesenterprise.sdk.tx.observer.starter.properties.TxObserverSchedulerProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import org.springframework.scheduling.support.ScheduledMethodRunnable
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

@Configuration
@Profile("!test")
@Import(
    TxObserverLockConfig::class,
    BlockInfoSynchronizerConfig::class,
    PartitionPollerConfig::class,
    PrivacyAvailabilityCheckConfig::class,
    TxQueueConfig::class,
    ForkResolverConfig::class,
    MetricsCollectorConfig::class,
    PartitionPausedOnTxIdCleanerConfig::class,
    PartitionCleanerConfig::class,
)
@EnableScheduling
@EnableConfigurationProperties(TxObserverSchedulerProperties::class)
class TxObserverSchedulerConfig : SchedulingConfigurer {
    @Autowired
    lateinit var txObserverSchedulerProperties: TxObserverSchedulerProperties

    @Autowired
    var scheduledBlockInfoSynchronizer: ScheduledBlockInfoSynchronizer? = null

    @Autowired
    var eventSubscriber: EventSubscriber? = null

    @Autowired
    lateinit var blockHistoryCleaner: BlockHistoryCleaner

    @Autowired
    lateinit var txObserverProperties: TxObserverProperties

    @Autowired
    lateinit var scheduledPartitionPoller: ScheduledPartitionPoller

    @Autowired
    lateinit var scheduledPrivacyChecker: ScheduledPrivacyChecker

    @Autowired
    lateinit var partitionPollerProperties: PartitionPollerProperties

    @Autowired
    lateinit var privacyAvailabilityCheckProperties: PrivacyAvailabilityCheckProperties

    @Autowired
    lateinit var scheduledTxQueueCleaner: ScheduledTxQueueCleaner

    @Autowired
    lateinit var scheduledPartitionPausedOnTxIdCleaner: ScheduledPartitionPausedOnTxIdCleaner

    @Autowired
    lateinit var partitionPausedOnTxIdCleanerProperties: PartitionPausedOnTxIdCleanerProperties

    @Autowired
    lateinit var scheduledPartitionCleaner: ScheduledPartitionCleaner

    @Autowired
    lateinit var partitionCleanerProperties: PartitionCleanerProperties

    @Autowired
    lateinit var queueCleanerProperties: QueueCleanerProperties

    @Autowired
    lateinit var scheduledForkResolver: ScheduledForkResolver

    @Autowired
    lateinit var forkResolverProperties: ForkResolverProperties

    @Autowired
    lateinit var scheduledMetricsCollector: ScheduledMetricsCollector

    @Autowired
    lateinit var metricsCollectorProperties: MetricsCollectorProperties

    @Bean
    fun observerScheduler() =
        ThreadPoolTaskScheduler().apply {
            poolSize = listOf(
                txObserverSchedulerProperties.poolSize,
                privacyAvailabilityCheckProperties.threadCount,
                partitionPollerProperties.threadCount
            ).sum()
            setThreadNamePrefix("tx-observer-pool-")
            initialize()
        }

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        taskRegistrar.setTaskScheduler(observerScheduler())
        taskRegistrar.apply {
            addBlockInfoSynchronizer()
            addEventSubscriber()
            addBlockHistoryCleaner()
            if (partitionPollerProperties.enabled)
                initPartitionPoller()
            if (privacyAvailabilityCheckProperties.enabled)
                initPrivacyChecker()
            addScheduledForkResolver()
            addQueueCleaner()
            addScheduledMetricsCollector()
            addPartitionPausedOnTxIdCleaner()
            addPartitionCleaner()
        }
    }

    private fun ScheduledTaskRegistrar.addScheduledMetricsCollector() {
        if (metricsCollectorProperties.enabled)
            addFixedDelayTask(
                scheduledMethodRunnable(
                    scheduledMetricsCollector,
                    ScheduledMetricsCollector::metricsCollector
                ),
                metricsCollectorProperties.fixedDelay
            )
    }

    private fun ScheduledTaskRegistrar.addQueueCleaner() {
        if (queueCleanerProperties.enabled)
            addCronTask(
                scheduledMethodRunnable(
                    scheduledTxQueueCleaner,
                    ScheduledTxQueueCleaner::cleanReadEnqueuedTx
                ),
                queueCleanerProperties.cleanCronExpression
            )
    }

    private fun ScheduledTaskRegistrar.addScheduledForkResolver() {
        if (forkResolverProperties.enabled)
            addFixedDelayTask(
                scheduledMethodRunnable(
                    scheduledForkResolver,
                    ScheduledForkResolver::resolveForkedTx
                ),
                forkResolverProperties.fixedDelay
            )
    }

    private fun ScheduledTaskRegistrar.addBlockHistoryCleaner() {
        if (txObserverProperties.enabled)
            addFixedDelayTask(
                scheduledMethodRunnable(
                    blockHistoryCleaner,
                    BlockHistoryCleaner::clean
                ),
                txObserverProperties.blockHistoryCleanDelay
            )
    }

    private fun ScheduledTaskRegistrar.addEventSubscriber() {
        eventSubscriber?.let { eventSubscriber ->
            if (txObserverProperties.enabled)
                addFixedDelayTask(
                    scheduledMethodRunnable(
                        eventSubscriber,
                        EventSubscriber::subscribe
                    ),
                    txObserverProperties.fixedDelay
                )
        }
    }

    private fun ScheduledTaskRegistrar.addBlockInfoSynchronizer() {
        scheduledBlockInfoSynchronizer?.let { scheduledBlockInfoSynchronizer ->
            addFixedDelayTask(
                scheduledMethodRunnable(
                    scheduledBlockInfoSynchronizer,
                    ScheduledBlockInfoSynchronizer::syncNodeBlockInfo
                ),
                txObserverProperties.fixedDelay
            )
        }
    }

    internal fun ScheduledTaskRegistrar.initPrivacyChecker() {
        repeat(privacyAvailabilityCheckProperties.threadCount) {
            addFixedDelayTask(
                scheduledMethodRunnable(
                    scheduledPrivacyChecker,
                    ScheduledPrivacyChecker::checkPrivacyAvailabilityWhileTheyExist
                ),
                privacyAvailabilityCheckProperties.fixedDelay
            )
        }
    }

    private fun ScheduledTaskRegistrar.initPartitionPoller() {
        repeat(partitionPollerProperties.threadCount) {
            addFixedDelayTask(
                scheduledMethodRunnable(
                    scheduledPartitionPoller,
                    ScheduledPartitionPoller::pollWhileHavingActivePartitions
                ),
                partitionPollerProperties.fixedDelay
            )
        }
    }

    private fun ScheduledTaskRegistrar.addPartitionPausedOnTxIdCleaner() {
        if (partitionPausedOnTxIdCleanerProperties.enabled) {
            addFixedDelayTask(
                scheduledMethodRunnable(
                    scheduledPartitionPausedOnTxIdCleaner,
                    ScheduledPartitionPausedOnTxIdCleaner::clear
                ),
                partitionPausedOnTxIdCleanerProperties.fixedDelay
            )
        }
    }

    private fun ScheduledTaskRegistrar.addPartitionCleaner() {
        if (partitionCleanerProperties.enabled) {
            addFixedDelayTask(
                scheduledMethodRunnable(
                    scheduledPartitionCleaner,
                    ScheduledPartitionCleaner::cleanEmptyPartitions
                ),
                partitionCleanerProperties.fixedDelay
            )
        }
    }

    companion object {
        private inline fun <reified T : Any> scheduledMethodRunnable(
            scheduled: T,
            kFunction1: KFunction<T>,
        ): ScheduledMethodRunnable =
            ScheduledMethodRunnable(
                scheduled,
                kFunction1.javaMethod ?: error("Cannot get method for ${T::class.simpleName}"),
            )
    }
}
