package com.wavesenterprise.sdk.tx.observer.starter

import com.wavesenterprise.sdk.tx.observer.common.conditional.ConditionalOnJpaMode
import com.wavesenterprise.sdk.tx.observer.starter.lock.LockConfig
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@ConditionalOnJpaMode
@Import(
    TxQueueConfig::class,
    BlockInfoSynchronizerConfig::class,
    PrivacyAvailabilityCheckConfig::class,
    PartitionPollerConfig::class,
    ForkResolverConfig::class,
    MetricsCollectorConfig::class,
    TxObserverSchedulerConfig::class,
    LockConfig::class,
    PartitionPausedOnTxIdCleanerConfig::class,
    PartitionCleanerConfig::class,
)
class JpaExecutorsConfig
