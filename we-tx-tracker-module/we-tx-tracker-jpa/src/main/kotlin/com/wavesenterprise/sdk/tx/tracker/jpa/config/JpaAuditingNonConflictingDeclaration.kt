package com.wavesenterprise.sdk.tx.tracker.jpa.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@Configuration
@ConditionalOnMissingBean(name = ["jpaAuditingHandler"])
@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
@Import(DateTimeProviderConfig::class)
class JpaAuditingNonConflictingDeclaration
