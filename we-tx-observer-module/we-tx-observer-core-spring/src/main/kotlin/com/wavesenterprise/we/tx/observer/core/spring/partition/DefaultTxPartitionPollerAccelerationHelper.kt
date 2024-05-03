package com.wavesenterprise.we.tx.observer.core.spring.partition

import com.wavesenterprise.we.tx.observer.api.cache.DEFAULT_TIME_BASED_CACHE_MANAGER_NAME
import com.wavesenterprise.we.tx.observer.core.spring.properties.PartitionPollerConfig
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import org.springframework.cache.annotation.Cacheable

open class DefaultTxPartitionPollerAccelerationHelper(
    val enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
    val partitionPollerProperties: PartitionPollerConfig,
) : TxPartitionPollerAccelerationHelper {
    @Cacheable("isAccelerationRequired", cacheManager = DEFAULT_TIME_BASED_CACHE_MANAGER_NAME)
    override fun isAccelerationRequired(): Boolean =
        enqueuedTxJpaRepository.countByStatus(EnqueuedTxStatus.NEW) >= partitionPollerProperties.accelerateAtQueueSize
}
