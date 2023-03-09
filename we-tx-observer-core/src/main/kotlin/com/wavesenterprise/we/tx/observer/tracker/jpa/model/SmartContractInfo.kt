package com.wavesenterprise.we.tx.observer.tracker.jpa.model

import com.wavesenterprise.we.tx.observer.util.TX_TRACKER_SCHEMA_NAME
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.OffsetDateTime
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(schema = TX_TRACKER_SCHEMA_NAME)
@EntityListeners(AuditingEntityListener::class)
data class SmartContractInfo(
    @Id
    var id: String,
    var imageHash: String,
    var image: String,
    var version: Int,
    var contractName: String,
    var sender: String,

    @OneToMany(mappedBy = "smartContractInfo")
    var txTrackInfos: MutableList<TxTrackInfo> = mutableListOf(),

    @CreatedDate
    var created: OffsetDateTime? = null,

    @LastModifiedDate
    var modified: OffsetDateTime? = null
)
