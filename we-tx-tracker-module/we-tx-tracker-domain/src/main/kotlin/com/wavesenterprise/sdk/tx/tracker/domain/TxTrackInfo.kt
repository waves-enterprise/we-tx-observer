package com.wavesenterprise.sdk.tx.tracker.domain

import com.fasterxml.jackson.databind.JsonNode
import com.wavesenterprise.sdk.tx.observer.common.jpa.util.JSONB_TYPE
import com.wavesenterprise.sdk.tx.observer.common.jpa.util.TX_TRACKER_SCHEMA_NAME
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLInsert
import org.hibernate.annotations.Type
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.OffsetDateTime

@Entity
@Table(schema = TX_TRACKER_SCHEMA_NAME)
@EntityListeners(AuditingEntityListener::class)
@SQLInsert(
    sql = """
    insert into $TX_TRACKER_SCHEMA_NAME.tx_track_info 
        (body, created, errors, meta, modified, contract_id, status, type, user_id, id) values
        (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) on conflict do nothing
"""
)
data class TxTrackInfo(
    @Id
    val id: String,

    @ManyToOne
    @JoinColumn(name = "contractId")
    var smartContractInfo: SmartContractInfo?,

    @Enumerated(EnumType.STRING)
    var status: TxTrackStatus = TxTrackStatus.PENDING,

    var type: Int,

    @Type(JsonType::class)
    @Column(columnDefinition = JSONB_TYPE)
    var body: JsonNode,

    @Type(JsonType::class)
    var errors: JsonNode? = null,

    @Type(JsonType::class)
    val meta: Map<String, Any>,

    @CreatedDate
    var created: OffsetDateTime? = null,

    @LastModifiedDate
    var modified: OffsetDateTime? = null,

    val userId: String? = null,

    @ManyToMany(
        cascade = [
            CascadeType.PERSIST,
            CascadeType.MERGE,
            CascadeType.REFRESH,
            CascadeType.DETACH
        ]
    )
    @JoinTable(
        schema = TX_TRACKER_SCHEMA_NAME,
        name = "track_info_business_object_info",
        joinColumns = [JoinColumn(name = "track_info_id", nullable = false)],
        inverseJoinColumns = [JoinColumn(name = "business_object_info_id", nullable = false)]
    )
    @SQLInsert(
        sql = """
        insert 
        into
            $TX_TRACKER_SCHEMA_NAME.track_info_business_object_info
            (track_info_id, business_object_info_id) 
        values
            (?, ?) on conflict do nothing
    """
    )
    val businessObjectInfos: List<TxTrackBusinessObjectInfo> = listOf(),
)
