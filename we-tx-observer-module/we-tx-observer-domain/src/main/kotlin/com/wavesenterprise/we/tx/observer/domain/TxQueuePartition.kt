package com.wavesenterprise.we.tx.observer.domain

import com.wavesenterprise.we.tx.observer.common.annotation.TX_OBSERVER_SCHEMA_NAME
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.OffsetDateTime
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Id
import javax.persistence.Table

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
