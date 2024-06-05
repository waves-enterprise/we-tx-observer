package com.wavesenterprise.sdk.tx.tracker.jpa.repository

import com.wavesenterprise.sdk.tx.tracker.domain.SmartContractInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SmartContractInfoJpaRepository : JpaRepository<SmartContractInfo, String>
