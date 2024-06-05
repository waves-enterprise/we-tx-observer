package com.wavesenterprise.sdk.tx.observer.domain

import com.wavesenterprise.sdk.tx.observer.common.jpa.util.TX_OBSERVER_SCHEMA_NAME
import org.hibernate.annotations.SQLInsert
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.OffsetDateTime
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.Version

@Entity
@Table(schema = TX_OBSERVER_SCHEMA_NAME)
@EntityListeners(AuditingEntityListener::class)
@SQLInsert(
    sql = """
    insert into $TX_OBSERVER_SCHEMA_NAME.block_height_info 
        (created_timestamp, current_height, prev_block_signature, update_timestamp, version, id) values
        (?, ?, ?, ?, ?, ?) on conflict do nothing
"""
)
data class BlockHeightInfo(
    @Id
    var id: String = "node",

    @Version
    var version: Long? = null,

    var currentHeight: Long,
    var prevBlockSignature: String? = null,
) {
    @CreatedDate
    lateinit var createdTimestamp: OffsetDateTime

    @LastModifiedDate
    lateinit var updateTimestamp: OffsetDateTime
}
