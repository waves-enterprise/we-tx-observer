package com.wavesenterprise.we.tx.observer.jpa.repository

import com.wavesenterprise.we.tx.observer.domain.BlockHeightReset
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BlockHeightResetRepository : JpaRepository<BlockHeightReset, String>
