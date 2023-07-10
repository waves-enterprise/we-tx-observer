package com.wavesenterprise.we.tx.tracker.jpa.repository

import com.wavesenterprise.we.tx.tracker.domain.SmartContractInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SmartContractInfoJpaRepository : JpaRepository<SmartContractInfo, String>
