package com.wavesenterprise.we.tx.observer.domain

import com.wavesenterprise.we.tx.observer.common.annotation.TX_OBSERVER_SCHEMA_NAME
import org.hibernate.annotations.SQLInsert
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.Version

@Entity
@Table(schema = TX_OBSERVER_SCHEMA_NAME)
@EntityListeners(AuditingEntityListener::class)
@SQLInsert(
    sql = """
    insert into $TX_OBSERVER_SCHEMA_NAME.block_height_info 
        (created_timestamp, current_height, node_alias, prev_block_signature, update_timestamp, version, id) values
        (?, ?, ?, ?, ?, ?, ?) on conflict do nothing
"""
)
data class BlockHeightInfo(
    @Id
    @GeneratedValue
    var id: UUID? = null,

    @Version
    var version: Long? = null,

    var nodeAlias: String? = null, // todo group by this prop and use as id
    var currentHeight: Long,
    var prevBlockSignature: String? = null,
) {
    @CreatedDate
    lateinit var createdTimestamp: OffsetDateTime

    @LastModifiedDate
    lateinit var updateTimestamp: OffsetDateTime
}
