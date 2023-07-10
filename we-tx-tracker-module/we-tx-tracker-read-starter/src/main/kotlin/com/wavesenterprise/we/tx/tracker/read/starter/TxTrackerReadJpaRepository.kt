package com.wavesenterprise.we.tx.tracker.read.starter

import com.wavesenterprise.we.tx.tracker.domain.TxTrackInfo
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.Repository
import org.springframework.stereotype.Repository as RepositoryAnnotation

@RepositoryAnnotation
interface TxTrackerReadJpaRepository : Repository<TxTrackInfo, String>, JpaSpecificationExecutor<TxTrackInfo> {
    fun findById(id: String): TxTrackInfo?
}
