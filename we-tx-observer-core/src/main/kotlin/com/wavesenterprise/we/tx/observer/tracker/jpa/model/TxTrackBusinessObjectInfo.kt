package com.wavesenterprise.we.tx.observer.tracker.jpa.model

import com.wavesenterprise.we.tx.observer.util.TX_TRACKER_SCHEMA_NAME
import org.hibernate.annotations.SQLInsert
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(schema = TX_TRACKER_SCHEMA_NAME)
@SQLInsert(
    sql = """
    insert into $TX_TRACKER_SCHEMA_NAME.tx_track_business_object_info (type, id) values (?, ?) on conflict do nothing
"""
)
data class TxTrackBusinessObjectInfo(
    @Id
    val id: String,
    val type: String
)
