package com.wavesenterprise.sdk.tx.tracker.domain

import com.wavesenterprise.sdk.tx.observer.common.jpa.util.TX_TRACKER_SCHEMA_NAME
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLInsert

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
    val type: String,
)
