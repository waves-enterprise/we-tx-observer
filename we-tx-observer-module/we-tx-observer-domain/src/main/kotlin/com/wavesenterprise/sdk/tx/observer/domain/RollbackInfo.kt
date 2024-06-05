package com.wavesenterprise.sdk.tx.observer.domain

import com.wavesenterprise.sdk.tx.observer.common.jpa.util.TX_OBSERVER_SCHEMA_NAME
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(schema = TX_OBSERVER_SCHEMA_NAME)
@EntityListeners(AuditingEntityListener::class)
data class RollbackInfo(
    @Id
    @GeneratedValue
    var id: UUID? = null,
    var toHeight: Long,
    var toBlockSignature: String,
) {
    @CreatedDate
    lateinit var createdTimestamp: OffsetDateTime
}
