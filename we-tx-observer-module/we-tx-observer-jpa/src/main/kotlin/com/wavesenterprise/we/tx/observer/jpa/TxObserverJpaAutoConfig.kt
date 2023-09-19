package com.wavesenterprise.we.tx.observer.jpa

import com.wavesenterprise.we.tx.observer.common.conditional.ConditionalOnJpaMode
import com.wavesenterprise.we.tx.observer.domain.BlockHeightInfo
import com.wavesenterprise.we.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.we.tx.observer.jpa.repository.BlockHeightJpaRepository
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered

@Configuration
@Import(TxObserverJpaConfig::class)
@ConditionalOnJpaMode
@AutoConfigurationPackage(
    basePackageClasses = [
        BlockHeightInfo::class,
        BlockHeightJpaRepository::class,
    ]
)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@AutoConfigureBefore(JpaRepositoriesAutoConfiguration::class, HibernateJpaAutoConfiguration::class)
class TxObserverJpaAutoConfig
