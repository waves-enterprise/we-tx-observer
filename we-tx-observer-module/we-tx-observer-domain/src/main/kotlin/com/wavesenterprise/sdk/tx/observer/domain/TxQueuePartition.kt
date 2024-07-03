package com.wavesenterprise.sdk.tx.observer.domain

import com.wavesenterprise.sdk.tx.observer.common.jpa.util.TX_OBSERVER_SCHEMA_NAME
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.OffsetDateTime

@Entity
@Table(schema = TX_OBSERVER_SCHEMA_NAME)
@EntityListeners(AuditingEntityListener::class)
data class TxQueuePartition(
    @Id
    val id: String,

    var priority: Int,

    var pausedOnTxId: String? = null, // set on create when having first 114?

    @CreatedDate
    var created: OffsetDateTime? = null,

    @LastModifiedDate
    var modified: OffsetDateTime? = null,
)
