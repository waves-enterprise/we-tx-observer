package com.wavesenterprise.sdk.tx.observer.domain

import com.wavesenterprise.sdk.tx.observer.common.jpa.util.TX_OBSERVER_SCHEMA_NAME
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(schema = TX_OBSERVER_SCHEMA_NAME)
data class BlockHeightReset(
    @Id
    var id: String = "node",
    val heightToReset: Long,
)
