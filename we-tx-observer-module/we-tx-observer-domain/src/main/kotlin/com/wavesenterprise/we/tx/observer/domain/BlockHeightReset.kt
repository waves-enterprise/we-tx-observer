package com.wavesenterprise.we.tx.observer.domain

import com.wavesenterprise.we.tx.observer.common.jpa.util.TX_OBSERVER_SCHEMA_NAME
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(schema = TX_OBSERVER_SCHEMA_NAME)
data class BlockHeightReset(
    @Id
    var id: String = "node",
    val heightToReset: Long,
)
