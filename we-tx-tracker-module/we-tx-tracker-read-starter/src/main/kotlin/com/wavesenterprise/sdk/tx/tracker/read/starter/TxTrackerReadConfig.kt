package com.wavesenterprise.sdk.tx.tracker.read.starter

import com.wavesenterprise.sdk.tx.observer.common.conditional.ConditionalOnTracker
import com.wavesenterprise.sdk.tx.tracker.api.TxTrackInfoService
import com.wavesenterprise.sdk.tx.tracker.read.starter.web.TxTrackInfoController
import com.wavesenterprise.sdk.tx.tracker.read.starter.web.service.TxTrackInfoServiceImpl
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType

@Configuration(proxyBeanMethods = false)
@ConditionalOnTracker
@ComponentScan(
    useDefaultFilters = false,
    basePackageClasses = [TxTrackInfoController::class],
    includeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [
                TxTrackInfoController::class,
            ],
        ),
    ],
)
@AutoConfigureAfter(TxTrackerReadJpaAutoConfig::class)
class TxTrackerReadConfig {

    @Bean
    @ConditionalOnMissingBean
    fun txTrackInfoService(txTrackerReadJpaRepository: TxTrackerReadJpaRepository): TxTrackInfoService =
        TxTrackInfoServiceImpl(txTrackerReadJpaRepository)
}
