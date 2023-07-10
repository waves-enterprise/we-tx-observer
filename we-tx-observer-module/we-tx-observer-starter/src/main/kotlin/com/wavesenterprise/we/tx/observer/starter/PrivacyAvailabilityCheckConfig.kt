package com.wavesenterprise.we.tx.observer.starter

import com.fasterxml.jackson.databind.ObjectMapper
import com.wavesenterprise.we.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.we.tx.observer.core.spring.component.OffsetProvider
import com.wavesenterprise.we.tx.observer.core.spring.component.RandomOffsetProvider
import com.wavesenterprise.we.tx.observer.core.spring.executor.NOT_AVAILABLE_PRIVACY_COUNT
import com.wavesenterprise.we.tx.observer.core.spring.executor.PRIVACY_BECAME_AVAILABLE_COUNT
import com.wavesenterprise.we.tx.observer.core.spring.executor.PrivacyAvailabilityChecker
import com.wavesenterprise.we.tx.observer.core.spring.executor.ScheduledPrivacyChecker
import com.wavesenterprise.we.tx.observer.core.spring.metrics.MetricContainerData
import com.wavesenterprise.we.tx.observer.core.spring.partition.PartitionHandler
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.we.tx.observer.starter.properties.PrivacyAvailabilityCheckProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(PrivacyAvailabilityCheckProperties::class)
class PrivacyAvailabilityCheckConfig {

    @Bean
    fun offsetProvider(): OffsetProvider = RandomOffsetProvider()

    @Bean
    fun privacyThatBecameAvailableMetric() = MetricContainerData(
        metricName = PRIVACY_BECAME_AVAILABLE_COUNT,
    )

    @Bean
    fun notAvailablePrivacyMetric() = MetricContainerData(
        metricName = NOT_AVAILABLE_PRIVACY_COUNT,
    )

    @Bean
    fun scheduledPrivacyChecker(
        privacyAvailabilityChecker: PrivacyAvailabilityChecker,
    ): ScheduledPrivacyChecker = ScheduledPrivacyChecker(
        privacyAvailabilityChecker = privacyAvailabilityChecker,
    )

    @Bean
    fun privacyAvailabilityChecker(
        enqueuedTxJpaRepository: EnqueuedTxJpaRepository,
        privateContentResolver: PrivateContentResolver,
        partitionHandler: PartitionHandler,
        objectMapper: ObjectMapper,
        properties: PrivacyAvailabilityCheckProperties,
        offsetProvider: OffsetProvider,
    ) = PrivacyAvailabilityChecker(
        enqueuedTxJpaRepository = enqueuedTxJpaRepository,
        privateContentResolver = privateContentResolver,
        partitionHandler = partitionHandler,
        offsetProvider = offsetProvider,
        objectMapper = objectMapper,
        properties = properties,
        privacyThatBecameAvailableMetric = privacyThatBecameAvailableMetric(),
    )
}
