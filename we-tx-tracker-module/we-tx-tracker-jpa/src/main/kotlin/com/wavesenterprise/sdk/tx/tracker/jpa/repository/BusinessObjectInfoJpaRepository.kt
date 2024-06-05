package com.wavesenterprise.sdk.tx.tracker.jpa.repository

import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackBusinessObjectInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BusinessObjectInfoJpaRepository : JpaRepository<TxTrackBusinessObjectInfo, String>
