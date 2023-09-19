package com.wavesenterprise.we.tx.observer.domain

import com.fasterxml.jackson.databind.JsonNode
import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import com.wavesenterprise.we.tx.observer.common.annotation.JSONB_TYPE
import com.wavesenterprise.we.tx.observer.common.annotation.TX_OBSERVER_SCHEMA_NAME
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.OffsetDateTime
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(schema = TX_OBSERVER_SCHEMA_NAME)
@EntityListeners(AuditingEntityListener::class)
@TypeDefs(
    TypeDef(name = JSONB_TYPE, typeClass = JsonBinaryType::class)
)
data class EnqueuedTx(
    @Id
    var id: String,

    @Type(type = JSONB_TYPE)
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
