package com.wavesenterprise.we.tx.tracker.jpa.config

import com.wavesenterprise.we.flyway.starter.FlywaySchema
import com.wavesenterprise.we.tx.observer.common.annotation.TX_TRACKER_SCHEMA_NAME
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.data.jpa.repository.JpaRepository
import javax.sql.DataSource

@Configuration
@ConditionalOnClass(JpaRepository::class)
@ConditionalOnBean(DataSource::class)
@Import(
    JpaAuditingNonConflictingDeclaration::class,
)
class TxTrackerJpaConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    fun txTrackerSchemaConfig(): FlywaySchema = object : FlywaySchema {
        override fun getName() = TX_TRACKER_SCHEMA_NAME
        override fun getLocation() = "__tx_tracker_schema"
    }
}
