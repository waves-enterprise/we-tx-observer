package com.wavesenterprise.we.tx.tracker.jpa.repository

import com.wavesenterprise.we.tx.tracker.domain.TxTrackBusinessObjectInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BusinessObjectInfoJpaRepository : JpaRepository<TxTrackBusinessObjectInfo, String>
