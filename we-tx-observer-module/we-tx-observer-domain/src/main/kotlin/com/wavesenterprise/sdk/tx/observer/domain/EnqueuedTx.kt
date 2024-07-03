package com.wavesenterprise.sdk.tx.observer.domain

import com.fasterxml.jackson.databind.JsonNode
import com.wavesenterprise.sdk.tx.observer.common.jpa.util.TX_OBSERVER_SCHEMA_NAME
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.OffsetDateTime

@Entity
@Table(schema = TX_OBSERVER_SCHEMA_NAME)
@EntityListeners(AuditingEntityListener::class)
data class EnqueuedTx(
    @Id
    var id: String,

    @Type(JsonType::class)
    var body: JsonNode,

    val txType: Int,

    @Enumerated(EnumType.STRING)
    var status: EnqueuedTxStatus,

    val blockHeight: Long,

    val positionInBlock: Int,

    val positionInAtomic: Int?,

    val atomicTxId: String?,

    var txTimestamp: OffsetDateTime,

    @ManyToOne
    @JoinColumn(name = "partitionId")
    val partition: TxQueuePartition,

    var available: Boolean,

    @CreatedDate
    var created: OffsetDateTime? = null,

    @LastModifiedDate
    var modified: OffsetDateTime? = null,

)
