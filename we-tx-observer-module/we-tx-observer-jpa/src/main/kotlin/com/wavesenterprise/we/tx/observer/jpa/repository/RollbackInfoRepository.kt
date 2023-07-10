package com.wavesenterprise.we.tx.observer.jpa.repository

import com.wavesenterprise.we.tx.observer.domain.RollbackInfo
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RollbackInfoRepository : CrudRepository<RollbackInfo, UUID>, JpaSpecificationExecutor<RollbackInfo>
