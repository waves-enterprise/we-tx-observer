package com.wavesenterprise.sdk.tx.observer.domain

import com.wavesenterprise.sdk.tx.observer.common.jpa.util.TX_OBSERVER_SCHEMA_NAME
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLInsert
import org.hibernate.annotations.SQLRestriction
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(schema = TX_OBSERVER_SCHEMA_NAME)
@EntityListeners(AuditingEntityListener::class)
@SQLInsert(
    sql = """
    insert into $TX_OBSERVER_SCHEMA_NAME.block_history
        (created_timestamp, deleted, height, signature, timestamp, id) values
        (?, ?, ?, ?, ?, ?) on conflict do nothing
"""
)
@SQLDelete(sql = "update $TX_OBSERVER_SCHEMA_NAME.block_history set deleted = true where id=?")
@SQLRestriction("deleted = false")
data class BlockHistory(
    @Id
    @GeneratedValue
    var id: UUID? = null,
    val signature: String,
    val height: Long,
    val timestamp: OffsetDateTime,
) {
    @CreatedDate
    lateinit var createdTimestamp: OffsetDateTime

    private val deleted = false
}
