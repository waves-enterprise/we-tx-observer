package com.wavesenterprise.we.tx.observer.api.jpa.model

import com.wavesenterprise.we.tx.observer.util.TX_OBSERVER_SCHEMA_NAME
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLInsert
import org.hibernate.annotations.Where
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
@SQLInsert(
    sql = """
    insert into $TX_OBSERVER_SCHEMA_NAME.block_history
        (created_timestamp, deleted, height, signature, timestamp, id) values
        (?, ?, ?, ?, ?, ?) on conflict do nothing
"""
)
@SQLDelete(sql = "update $TX_OBSERVER_SCHEMA_NAME.block_history set deleted = true where id=?")
@Where(clause = "deleted = false")
data class BlockHistory(
    @Id
    @GeneratedValue
    var id: UUID? = null,
    val signature: String,
    val height: Long,
    val timestamp: OffsetDateTime
) {
    @CreatedDate
    lateinit var createdTimestamp: OffsetDateTime

    private val deleted = false
}
