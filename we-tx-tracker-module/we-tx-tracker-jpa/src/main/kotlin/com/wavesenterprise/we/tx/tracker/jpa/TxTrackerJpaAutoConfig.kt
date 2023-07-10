package com.wavesenterprise.we.tx.tracker.jpa

import com.wavesenterprise.we.tx.observer.common.conditional.ConditionalOnJpaMode
import com.wavesenterprise.we.tx.observer.common.conditional.ConditionalOnTracker
import com.wavesenterprise.we.tx.tracker.domain.TxTrackInfo
import com.wavesenterprise.we.tx.tracker.jpa.config.TxTrackerJpaConfig
import com.wavesenterprise.we.tx.tracker.jpa.repository.TxTrackerJpaRepository
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered

@Configuration
@Import(TxTrackerJpaConfig::class)
@ConditionalOnJpaMode
@ConditionalOnTracker
@AutoConfigurationPackage(
    basePackageClasses = [
        TxTrackInfo::class,
        TxTrackerJpaRepository::class,
    ]
)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@AutoConfigureBefore(JpaRepositoriesAutoConfiguration::class, HibernateJpaAutoConfiguration::class)
class TxTrackerJpaAutoConfig
