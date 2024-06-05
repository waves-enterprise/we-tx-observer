package com.wavesenterprise.sdk.tx.observer.jpa.repository

import com.wavesenterprise.sdk.tx.observer.domain.BlockHeightReset
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BlockHeightResetRepository : JpaRepository<BlockHeightReset, String>
