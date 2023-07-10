package com.wavesenterprise.we.tx.observer.jpa.config

import com.wavesenterprise.we.tx.observer.common.annotation.TX_OBSERVER_SCHEMA_NAME
import com.wavesenterprise.we.tx.observer.common.tx.executor.TxExecutor
import com.wavesenterprise.we.tx.observer.common.tx.executor.TxExecutorImpl
import com.wavesplatform.we.flyway.schema.starter.FlywaySchema
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.transaction.support.TransactionTemplate
import javax.sql.DataSource

@Configuration
@ConditionalOnClass(JpaRepository::class)
@ConditionalOnBean(DataSource::class)
@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
@Import(DateTimeProviderConfig::class)
class TxObserverJpaConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun txObserverSchemaConfig(): FlywaySchema = object : FlywaySchema {
        override fun getName() = TX_OBSERVER_SCHEMA_NAME
        override fun getLocation() = "__tx_observer_schema"
    }

    @Bean
    fun txExecutor(
        transactionTemplate: TransactionTemplate,
    ): TxExecutor = TxExecutorImpl(
        transactionTemplate = transactionTemplate,
    )
}
