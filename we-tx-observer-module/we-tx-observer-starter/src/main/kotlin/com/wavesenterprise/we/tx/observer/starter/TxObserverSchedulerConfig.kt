package com.wavesenterprise.we.tx.observer.starter

import com.wavesenterprise.we.tx.observer.core.spring.executor.ScheduledForkResolver
import com.wavesenterprise.we.tx.observer.core.spring.executor.ScheduledPartitionPausedOnTxIdCleaner
import com.wavesenterprise.we.tx.observer.core.spring.executor.ScheduledPartitionPoller
import com.wavesenterprise.we.tx.observer.core.spring.executor.ScheduledPrivacyChecker
import com.wavesenterprise.we.tx.observer.core.spring.executor.ScheduledTxQueueCleaner
import com.wavesenterprise.we.tx.observer.core.spring.executor.poller.ScheduledBlockInfoSynchronizer
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.EventSubscriber
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.BlockHistoryCleaner
import com.wavesenterprise.we.tx.observer.core.spring.metrics.ScheduledMetricsCollector
import com.wavesenterprise.we.tx.observer.starter.properties.ForkResolverProperties
import com.wavesenterprise.we.tx.observer.starter.properties.MetricsCollectorProperties
import com.wavesenterprise.we.tx.observer.starter.properties.PartitionPausedOnTxIdCleanerProperties
import com.wavesenterprise.we.tx.observer.starter.properties.PartitionPollerProperties
import com.wavesenterprise.we.tx.observer.starter.properties.PrivacyAvailabilityCheckProperties
import com.wavesenterprise.we.tx.observer.starter.properties.QueueCleanerProperties
import com.wavesenterprise.we.tx.observer.starter.properties.TxObserverProperties
import com.wavesenterprise.we.tx.observer.starter.properties.TxObserverSchedulerProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.support.CronTrigger
import org.springframework.scheduling.support.PeriodicTrigger
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
)
@EnableScheduling
@EnableConfigurationProperties(TxObserverSchedulerProperties::class)
class TxObserverSchedulerConfig {
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
    lateinit var queueCleanerProperties: QueueCleanerProperties

    @Autowired
    lateinit var scheduledForkResolver: ScheduledForkResolver

    @Autowired
    lateinit var forkResolverProperties: ForkResolverProperties

    @Autowired
    lateinit var scheduledMetricsCollector: ScheduledMetricsCollector

    @Autowired
    lateinit var metricsCollectorProperties: MetricsCollectorProperties

    @Bean // TODO mb refactor Duration to cron+, separate inits+
    fun observerScheduler() =
        ThreadPoolTaskScheduler().apply {
            poolSize = listOf(
                txObserverSchedulerProperties.poolSize,
                privacyAvailabilityCheckProperties.threadCount,
                partitionPollerProperties.threadCount
            ).sum()
            setThreadNamePrefix("tx-observer-pool-")
            initialize()
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
        }

    private fun ThreadPoolTaskScheduler.addScheduledMetricsCollector() {
        if (metricsCollectorProperties.enabled)
            schedule(
                scheduledMethodRunnable(
                    scheduledMetricsCollector,
                    ScheduledMetricsCollector::metricsCollector
                ),
                PeriodicTrigger(metricsCollectorProperties.fixedDelay.toMillis())
            )
    }

    private fun ThreadPoolTaskScheduler.addQueueCleaner() {
        if (queueCleanerProperties.enabled)
            schedule(
                scheduledMethodRunnable(
                    scheduledTxQueueCleaner,
                    ScheduledTxQueueCleaner::cleanReadEnqueuedTx
                ),
                CronTrigger(queueCleanerProperties.cleanCronExpression)
            )
    }

    private fun ThreadPoolTaskScheduler.addScheduledForkResolver() {
        if (forkResolverProperties.enabled)
            schedule(
                scheduledMethodRunnable(
                    scheduledForkResolver,
                    ScheduledForkResolver::resolveForkedTx
                ),
                PeriodicTrigger(forkResolverProperties.fixedDelay.toMillis())
            )
    }

    private fun ThreadPoolTaskScheduler.addBlockHistoryCleaner() {
        if (txObserverProperties.enabled)
            schedule(
                scheduledMethodRunnable(
                    blockHistoryCleaner,
                    BlockHistoryCleaner::clean
                ),
                PeriodicTrigger(txObserverProperties.blockHistoryCleanDelay.toMillis())
            )
    }

    private fun ThreadPoolTaskScheduler.addEventSubscriber() {
        eventSubscriber?.let { eventSubscriber ->
            if (txObserverProperties.enabled)
                schedule(
                    scheduledMethodRunnable(
                        eventSubscriber,
                        EventSubscriber::subscribe
                    ),
                    PeriodicTrigger(txObserverProperties.fixedDelay.toMillis())
                )
        }
    }

    private fun ThreadPoolTaskScheduler.addBlockInfoSynchronizer() {
        scheduledBlockInfoSynchronizer?.let { scheduledBlockInfoSynchronizer ->
            if (txObserverProperties.enabled)
                schedule(
                    scheduledMethodRunnable(
                        scheduledBlockInfoSynchronizer,
                        ScheduledBlockInfoSynchronizer::syncNodeBlockInfo
                    ),
                    PeriodicTrigger(txObserverProperties.fixedDelay.toMillis())
                )
        }
    }

    internal fun ThreadPoolTaskScheduler.initPrivacyChecker() {
        repeat(privacyAvailabilityCheckProperties.threadCount) {
            schedule(
                scheduledMethodRunnable(
                    scheduledPrivacyChecker,
                    ScheduledPrivacyChecker::checkPrivacyAvailabilityWhileTheyExist
                ),
                PeriodicTrigger(privacyAvailabilityCheckProperties.fixedDelay.toMillis())
            )
        }
    }

    private fun ThreadPoolTaskScheduler.initPartitionPoller() {
        repeat(partitionPollerProperties.threadCount) {
            schedule(
                scheduledMethodRunnable(
                    scheduledPartitionPoller,
                    ScheduledPartitionPoller::pollWhileHavingActivePartitions
                ),
                PeriodicTrigger(partitionPollerProperties.fixedDelay.toMillis())
            )
        }
    }

    private fun ThreadPoolTaskScheduler.addPartitionPausedOnTxIdCleaner() {
        if (partitionPausedOnTxIdCleanerProperties.enabled) {
            schedule(
                scheduledMethodRunnable(
                    scheduledPartitionPausedOnTxIdCleaner,
                    ScheduledPartitionPausedOnTxIdCleaner::clear
                ),
                PeriodicTrigger(partitionPausedOnTxIdCleanerProperties.fixedDelay.toMillis())
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
