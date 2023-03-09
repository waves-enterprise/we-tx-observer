package com.wavesenterprise.we.tx.observer.api.jpa.model

import com.wavesenterprise.we.tx.observer.util.TX_OBSERVER_SCHEMA_NAME
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(schema = TX_OBSERVER_SCHEMA_NAME)
data class Lock(
    @Id
    var name: String
)
