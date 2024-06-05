package com.wavesenterprise.sdk.tx.tracker.domain

import com.fasterxml.jackson.databind.JsonNode
import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import com.wavesenterprise.sdk.tx.observer.common.jpa.util.JSONB_TYPE
import com.wavesenterprise.sdk.tx.observer.common.jpa.util.TX_TRACKER_SCHEMA_NAME
import org.hibernate.annotations.SQLInsert
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.OffsetDateTime
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(schema = TX_TRACKER_SCHEMA_NAME)
@EntityListeners(AuditingEntityListener::class)
@TypeDefs(
    TypeDef(name = JSONB_TYPE, typeClass = JsonBinaryType::class)
)
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

    @Type(type = JSONB_TYPE)
    @Column(columnDefinition = JSONB_TYPE)
    var body: JsonNode,

    @Type(type = JSONB_TYPE)
    var errors: JsonNode? = null,

    @Type(type = JSONB_TYPE)
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
