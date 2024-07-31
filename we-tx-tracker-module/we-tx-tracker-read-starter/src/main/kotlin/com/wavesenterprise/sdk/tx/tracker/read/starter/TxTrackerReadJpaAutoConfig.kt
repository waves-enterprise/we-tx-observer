package com.wavesenterprise.sdk.tx.tracker.read.starter

import com.wavesenterprise.sdk.tx.observer.common.conditional.ConditionalOnJpaMode
import com.wavesenterprise.sdk.tx.observer.common.conditional.ConditionalOnTracker
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackInfo
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ConditionalOnJpaMode
@ConditionalOnTracker
@AutoConfigurationPackage(
    basePackageClasses = [
        TxTrackInfo::class,
        TxTrackerReadJpaRepository::class,
    ],
)
@AutoConfigureAfter(DataSourceAutoConfiguration::class)
@AutoConfigureBefore(JpaRepositoriesAutoConfiguration::class, HibernateJpaAutoConfiguration::class)
class TxTrackerReadJpaAutoConfig
