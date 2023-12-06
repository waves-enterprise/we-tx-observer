package com.wavesenterprise.we.tx.observer.starter

import com.wavesenterprise.we.tx.observer.common.jpa.util.TX_OBSERVER_SCHEMA_NAME
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
@ConditionalOnProperty(
    name = ["tx-observer.lock-enabled"],
    havingValue = "true",
)

@EnableSchedulerLock(
    interceptMode = EnableSchedulerLock.InterceptMode.PROXY_SCHEDULER,
    defaultLockAtMostFor = "PT10S",
)
class TxObserverLockConfig {

    @Bean
    @ConditionalOnMissingBean
    fun lockProvider(dataSource: DataSource): LockProvider {
        return JdbcTemplateLockProvider(dataSource, "$TX_OBSERVER_SCHEMA_NAME.$LOCK_TABLE_NAME")
    }

    companion object {
        const val LOCK_TABLE_NAME = "shedlock"
    }
}
